import { Component, OnInit } from '@angular/core'
import { ActivatedRoute, ParamMap } from '@angular/router'
import { switchMap } from 'rxjs/operators'
import { ServerApiService, parseDate, isExpired, isExpireSoon, DomainPayments, Subscription } from '../server-api.service'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import * as moment from 'moment'
import { Sort } from '@angular/material/sort'

@Component( {
    selector: 'app-domain',
    templateUrl: './domain.component.html',
    styleUrls: ['./domain.component.css']
} )
export class DomainComponent implements OnInit {

    domainName: string

    isViewDomain: boolean = false

    localExpire: string = null

    isExpired: boolean = true

    isExpireSoon: boolean = false

    payments: PaymentRow[] = []

    sortedPayments: PaymentRow[] = []

    constructor(
        private authentication: AuthenticationService,
        private route: ActivatedRoute,
        private serverApiService: ServerApiService,
    ) { }

    ngOnInit() {
        this.authentication.authenticationState().subscribe(( user: SocialUser ) => {

            // Nothing further if not logged in
            if ( !user ) {
                return
            }

            // Specify the domain
            this.domainName = this.route.snapshot.paramMap.get( 'domain' )

            // Obtain email address to determine if logged in user
            const userEmail = user.email

            // Load the subscriptions for domain
            this.serverApiService.getDomainSubscriptions( this.domainName ).subscribe(( domainPayments: DomainPayments ) => {

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
                    const isPaidByYou = ( userEmail == paidByEmail )
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
                this.sortPayments( {
                    active: 'date',
                    direction: 'desc'
                } )
                if ( this.sortedPayments.length > 0 ) {
                    this.sortedPayments[0].isSubscriptionCompletion = true
                }

            }, ( error: any ) => {
                console.log( 'TODO error: ', error )
            } )
        } )
    }

    sortPayments( sort: Sort ) {
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