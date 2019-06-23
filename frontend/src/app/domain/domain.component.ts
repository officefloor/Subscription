import { Component, OnInit, OnDestroy } from '@angular/core'
import { ActivatedRoute, ParamMap } from '@angular/router'
import { switchMap } from 'rxjs/operators'
import { ServerApiService, parseDate, isExpired, isExpireSoon, DomainPayments, Subscription, Initialisation } from '../server-api.service'
import { InitialiseService } from '../initialise.service'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import * as moment from 'moment'
import { Sort } from '@angular/material/sort'
import { LatestDomainPaymentsService, DomainPaymentsListener } from '../latest-domain-payments.service'
import { AlertService } from '../alert.service'

@Component( {
    selector: 'app-domain',
    templateUrl: './domain.component.html',
    styleUrls: ['./domain.component.css']
} )
export class DomainComponent implements OnInit, OnDestroy, DomainPaymentsListener {

    paymentCurrency: string

    domainName: string

    isViewDomain: boolean = false

    localExpire: string = null

    isExpired: boolean = true

    isExpireSoon: boolean = false

    private userEmail: string = null

    private payments: PaymentRow[] = []

    sortedPayments: PaymentRow[] = []

    private currentSort: Sort = {
        active: 'date',
        direction: 'desc'
    }

    constructor(
        private initialiseServer: InitialiseService,
        private authentication: AuthenticationService,
        private route: ActivatedRoute,
        private serverApiService: ServerApiService,
        private latestDomainPaymentsService: LatestDomainPaymentsService,
        private alertService: AlertService,
    ) { }

    ngOnInit(): void {

        // Specify the domain
        this.domainName = this.route.snapshot.paramMap.get( 'domain' )

        // Register for latest domain payments
        this.latestDomainPaymentsService.addListener( this )

        // Load payment currency
        this.initialiseServer.initialisation().subscribe(( initialisation: Initialisation ) => this.paymentCurrency = initialisation.paypalCurrency )

        // Only load if authenticated
        this.authentication.authenticationState().subscribe(( user: SocialUser ) => {

            // Nothing further if not logged in
            if ( !user ) {
                return
            }

            // Obtain email address to determine if logged in user
            this.userEmail = user.email

            // Load the subscriptions for domain
            this.serverApiService.getDomainSubscriptions( this.domainName ).subscribe(( domainPayments: DomainPayments ) => {

                // Load the domain payments
                this.latestDomainPayments( domainPayments )

            }, ( error: any ) => {

                // Handle no access to domain
                if ( error.status && error.status === 403 ) {
                    // Swallow error, as may pay to get access
                    console.log( 'No access to domain', this.domainName )
                    return
                }

                // Alert regarding the generic error
                this.alertService.error( error )
            } )
        } )
    }

    ngOnDestroy(): void {
        this.latestDomainPaymentsService.removeListener( this )
    }

    latestDomainPayments( domainPayments: DomainPayments ) {

        // Determine if payments
        if ( !domainPayments.payments || ( domainPayments.payments.length === 0 ) ) {
            this.isViewDomain = false
            return
        }

        // View the domain
        this.isViewDomain = true
        const expireMoment = parseDate( domainPayments.expiresDate )
        this.localExpire = expireMoment.fromNow()

        // Determine how long ago expired
        this.isExpired = isExpired( expireMoment )
        this.isExpireSoon = isExpireSoon( expireMoment )

        // Load the payments
        const LOCAL_DATE_FORMAT = 'D MMM YYYY'
        this.payments = []
        let startDate: string = null
        for ( let payment of domainPayments.payments ) {
            const extendsToMoment = parseDate( payment.extendsToDate )
            const sortDate = extendsToMoment.unix()
            const extendsToDate = extendsToMoment.format( LOCAL_DATE_FORMAT )
            const paymentDate = parseDate( payment.paymentDate ).format( LOCAL_DATE_FORMAT )
            const paidByName = payment.paidByName || ''
            const paidByEmail = payment.paidByEmail || ''
            const isPaidByYou = ( this.userEmail == paidByEmail )
            const paidBy = isPaidByYou ? 'Yourself' : paidByName + ' - ' + paidByEmail
            const paymentOrderId = payment.paymentOrderId
            const paymentReceipt = payment.paymentReceipt
            const paymentAmount = payment.paymentAmount ? payment.paymentAmount / 100 : null

            // Determine first start date
            if ( !startDate || payment.isRestartSubscription ) {
                startDate = paymentDate
            }

            // Add the payment
            const paymentRow: PaymentRow = {
                sortDate: sortDate,
                subscriptionStartDate: startDate,
                subscriptionEndDate: extendsToDate,
                paymentDate: paymentDate,
                isRestartSubscription: payment.isRestartSubscription,
                isPaidByYou: isPaidByYou,
                paidBy: paidBy,
                paymentOrderId: paymentOrderId,
                paymentReceipt: paymentReceipt,
                paymentAmount: paymentAmount,
                isSubscriptionCompletion: false,
            }
            this.payments.push( paymentRow )

            // Specify start date for next row
            startDate = extendsToDate
        }

        // Sort payments (with first being subscription completion)
        this.sortPayments( this.currentSort )
        if ( this.sortedPayments.length > 0 ) {
            this.sortedPayments[0].isSubscriptionCompletion = true
        }
    }

    sortPayments( sort: Sort ) {
        this.currentSort = sort
        const data: PaymentRow[] = this.payments.slice()
        if ( !sort.active || sort.direction === '' ) {
            this.sortedPayments = data
            return
        }

        this.sortedPayments = data.sort(( a: PaymentRow, b: PaymentRow ) => {
            const isAsc = sort.direction === 'asc'
            switch ( sort.active ) {
                case 'date': return this.compare( a.sortDate, b.sortDate, isAsc )
                default: return 0;
            }
        } )
    }

    compare( a: number | string, b: number | string, isAsc: boolean ) {
        return ( a < b ? -1 : 1 ) * ( isAsc ? 1 : -1 )
    }

}

class PaymentRow {
    sortDate: number
    subscriptionStartDate: string
    subscriptionEndDate: string
    isPaidByYou: boolean
    paidBy: string
    paymentOrderId: string
    paymentReceipt: string
    paymentAmount: number
    paymentDate: string
    isRestartSubscription: boolean
    isSubscriptionCompletion: boolean
}