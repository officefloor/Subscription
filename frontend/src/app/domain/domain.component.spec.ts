import { DomainComponent } from './domain.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { CheckoutComponent } from '../checkout/checkout.component'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { SocialUser } from "angularx-social-login"
import { of } from 'rxjs'
import { InitialiseService } from '../initialise.service'
import { AuthenticationService } from '../authentication.service'
import { ActivatedRoute } from '@angular/router'
import { DomainPayments, Subscription, formatDate, parseDate } from '../server-api.service'
import * as moment from 'moment'
import { MatLinkPreviewModule } from '@angular-material-extensions/link-preview'

describe( 'DomainComponent', () => {

    const USER_EMAIL: string = 'daniel@officefloor.org'

    let initialiseServiceSpy: any
    let authenticationServiceSpy: any
    let activatedRoute: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        initialiseServiceSpy = jasmine.createSpyObj( 'InitialiseService', ['initialisation'] )
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['authenticationState'] )
        activatedRoute = { snapshot: { paramMap: { get: () => 'officefloor.org' } } }

        TestBed.configureTestingModule( {
            declarations: [DomainComponent, CheckoutComponent],
            imports: [HttpClientTestingModule, MatLinkPreviewModule.forRoot()],
            providers: [
                { provide: InitialiseService, useValue: initialiseServiceSpy },
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
                { provide: ActivatedRoute, useValue: activatedRoute },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    afterEach(() => httpTestingController.verify() )

    function newComponent( user: SocialUser = null, paypalCurrency: string = 'AUD' ): { component: DomainComponent, fixture: ComponentFixture<DomainComponent>, dom: HTMLElement } {
        initialiseServiceSpy.initialisation.and.returnValue( of( { paypalCurrency: paypalCurrency } ) )
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( DomainComponent )
        const component = fixture.componentInstance
        const dom = fixture.nativeElement
        fixture.detectChanges()
        return { component, fixture, dom }
    }

    function newDomainPayments( subscriptions: Array<Subscription> ): DomainPayments {
        let expiresDate = moment()
        subscriptions.forEach(( subscription: Subscription ) => {
            const subscriptionDate = parseDate( subscription.extendsToDate )
            if ( expiresDate.isBefore( subscriptionDate ) ) {
                expiresDate = subscriptionDate
            }
        } )
        const domainPayments: DomainPayments = {
            domainName: 'officefloor.org',
            expiresDate: formatDate( expiresDate ),
            payments: subscriptions
        }
        return domainPayments
    }

    interface PaymentRow extends Subscription {
        startDate: string
        paidByDetails: string
    }

    function newPaymentRow( paymentDate: moment.Moment, name: string = null, paidByDetails: string = ' - ', isRestart: boolean = false ): PaymentRow {
        const paymentRow: PaymentRow = {
            paymentDate: formatDate( paymentDate ),
            startDate: formatDate( paymentDate ),
            extendsToDate: formatDate( paymentDate.clone().add( 1, 'year' ) ),
            isRestartSubscription: isRestart,
            paidByName: name,
            paidByEmail: name ? name + '@officefloor.org' : null,
            paidByDetails: paidByDetails,
            paymentAmount: name ? 500 : null,
            paymentOrderId: name ? 'ORDER-' + name : null,
            paymentReceipt: name ? 'RECEIPT-' + name : null,
        }
        return paymentRow
    }

    function expectSubscriptions( component: DomainComponent, dom: HTMLElement, payments: Array<PaymentRow> ) {

        // Ensure correct number of subscriptions
        expect( component.sortedPayments.length ).toEqual( payments.length )
        if ( component.sortedPayments.length === 0 ) {
            return // no rows, so nothing to validate
        }

        // Find the table
        const tableBody = dom.querySelector( 'tbody' )
        expect( tableBody ).toBeTruthy( 'Can not find table body' )

        // Verify subscriptions correctly rendered to screen
        const displayDate = ( date ) => parseDate( date ).format( 'D MMM YYYY' )
        for ( let rowIndex = 0; rowIndex < payments.length; rowIndex++ ) {
            const payment: PaymentRow = payments[rowIndex]

            // Confirm row for the subscription
            const row = tableBody.childNodes[rowIndex + 1] // +1 avoid #comment
            expect( row ).toBeTruthy( 'Missing row ' + rowIndex + ': ' + row )
            expect( row.nodeName ).toEqual( 'TR', row )

            // Validate values of row
            const cellValues: Array<string> = [
                displayDate( payment.startDate ),
                displayDate( payment.extendsToDate ),
                payment.paidByDetails,
            ]
            for ( let i = 0; i < cellValues.length; i++ ) {
                const cellValue = cellValues[i]

                // Confirm the cell value
                const cell = row.childNodes[i]
                expect( cell ).toBeTruthy( 'Missing cell ' + i + ' for row ' + rowIndex )
                expect( cell.textContent ).toEqual( cellValue, 'Incorrect cell ' + i + ' for row ' + rowIndex )
            }
        }
    }

    it( 'correct domain', () => {
        const { component } = newComponent()
        expect( component.domainName ).toEqual( 'officefloor.org' )

        // Ignore loading link previews
        httpTestingController.match(( req ) => req.url === 'https://api.linkpreview.net/' )
    } )

    it( 'paypal currency', () => {
        const { component } = newComponent()
        expect( component ).toBeTruthy()
        expect( component.paymentCurrency ).toEqual( 'AUD' )

        // Ignore loading link previews
        httpTestingController.match(( req ) => req.url === 'https://api.linkpreview.net/' )
    } )

    function testSubscriptions( paymentRows: Array<PaymentRow> ): void {
        const user = new SocialUser()
        user.email = USER_EMAIL
        const { component, fixture, dom } = newComponent( user )
        const req = httpTestingController.expectOne( '/subscriptions/domain/officefloor.org' )
        expect( req.request.method ).toEqual( 'GET' )
        req.flush( newDomainPayments( paymentRows.map(( payment: PaymentRow ) => {
            // Ignore loading link previews
            httpTestingController.match(( req ) => req.url === 'https://api.linkpreview.net/' )
            
            // Return the subscription
            const clone: Subscription = {
                paymentDate: payment.paymentDate,
                extendsToDate: payment.extendsToDate,
                isRestartSubscription: payment.isRestartSubscription,
                paidByName: payment.paidByName,
                paidByEmail: payment.paidByEmail,
                paymentAmount: payment.paymentAmount,
                paymentOrderId: payment.paymentOrderId,
                paymentReceipt: payment.paymentReceipt
            }
            return clone
        } ) ) )
        fixture.detectChanges()
        expect( component.isExpired ).toEqual( false, 'should not be expired' )
        expect( component.isExpireSoon ).toEqual( false, 'should not expire soon' )
        expectSubscriptions( component, dom, paymentRows )
    }

    it( 'single payment', () => testSubscriptions( [newPaymentRow( moment() )] ) )

    it( 'multiple payments', () => {
        const expiry = moment().add( 2, 'year' )
        testSubscriptions( [
            newPaymentRow( expiry.clone() ),
            newPaymentRow( expiry.clone().subtract( 1, 'year' ) ),
            newPaymentRow( expiry.clone().subtract( 2, 'year' ) ),
        ] )
    } )

    it( 'payment by yourself', () => testSubscriptions( [newPaymentRow( moment(), 'daniel', 'Yourself' )] ) )

    it( 'payment by another', () => testSubscriptions( [newPaymentRow( moment(), 'another', 'another - another@officefloor.org' )] ) )

})