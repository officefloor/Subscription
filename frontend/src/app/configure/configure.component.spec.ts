import { ConfigureComponent } from './configure.component'
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { ReactiveFormsModule } from '@angular/forms'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { Observable, BehaviorSubject, of } from 'rxjs'
import { AlertService, AlertListener } from '../alert.service'
import { Configuration, Administrator } from '../server-api.service'
import { expectValue, expectChecked, setValue, setChecked } from '../app.component.spec'

describe( 'ConfigureComponent', () => {

    let authenticationServiceSpy: any

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    let alertService: AlertService
    let alertListener: AlertListener
    let successMessage: string
    let errorAlert: any

    const administrator: Administrator = {
        googleId: 'GOOGLE_ADMIN_ID',
        notes: 'MOCK_NOTES'
    }
    const configuration: Configuration = {
        googleClientId: 'GOOGLE_CLIENT_ID',
        administrators: [administrator],
        paypalEnvironment: 'sandbox',
        paypalClientId: 'PAYPAL_CLIENT_ID',
        paypalClientSecret: 'PAYPAL_CLIENT_SECRET',
        paypalCurrency: 'PAYPAL_CURRENCY',
        paypalInvoiceIdTemplate: 'template'
    }

    beforeEach( waitForAsync(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['initialise', 'authenticationState'] )
        TestBed.configureTestingModule( {
            declarations: [ConfigureComponent],
            imports: [ReactiveFormsModule, HttpClientTestingModule],
            providers: [
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
            ],
        } ).compileComponents()
    } ) )

    beforeEach(() => {
        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )

        // Listen on errors
        alertService = TestBed.get( AlertService )
        successMessage = null
        errorAlert = null
        expect( alertListener ).toBeFalsy( 'should not have previous alert listener' )
        alertListener = {
            success: ( message ) => successMessage = message,
            error: ( message ) => errorAlert = message,
        }
        alertService.addListener( alertListener )
    } )

    afterEach(() => {
        alertService.removeListener( alertListener )
        alertListener = null
        httpTestingController.verify()
        expect( errorAlert ).toBeNull( 'should not end in error' )
    } )

    function newComponent( user: Observable<SocialUser> = of( null ) ): { component: ConfigureComponent, fixture: ComponentFixture<ConfigureComponent>, dom: HTMLElement } {
        authenticationServiceSpy.initialise.and.returnValue( of( {} ) )
        authenticationServiceSpy.authenticationState.and.returnValue( user )
        const fixture = TestBed.createComponent( ConfigureComponent )
        fixture.detectChanges()
        const component = fixture.componentInstance
        const dom = fixture.nativeElement
        return { component, fixture, dom }
    }

    it( 'not logged in', ( done: DoneFn ) => {
        const { component, fixture } = newComponent()
        expect( component ).toBeTruthy()
        fixture.detectChanges()
        fixture.whenStable().then(() => {
            expect( errorAlert ).toEqual( 'Must be logged in to access configuration' )
            errorAlert = null // checked
            done()
        } ).catch( fail )
    } )

    it( 'display configuration', ( done: DoneFn ) => {
        const { component, fixture, dom } = newComponent( of( new SocialUser() ) )
        const req = httpTestingController.expectOne( '/configuration' )
        expect( req.request.method ).toEqual( 'GET' )
        req.flush( configuration )
        fixture.detectChanges()
        fixture.whenStable().then(() => {
            expectValue( dom, '#googleClientId', 'GOOGLE_CLIENT_ID' )
            expectValue( dom, '.googleId', 'GOOGLE_ADMIN_ID' )
            expectValue( dom, '.notes', 'MOCK_NOTES' )
            expectChecked( dom, '#paypalEnvironmentSandbox', true )
            expectChecked( dom, '#paypalEnvironmentLive', false )
            expectValue( dom, '#paypalClientId', 'PAYPAL_CLIENT_ID' )
            expectValue( dom, '#paypalClientSecret', 'PAYPAL_CLIENT_SECRET' )
            expectValue( dom, '#paypalInvoiceIdTemplate', 'template' )
            done()
        }, fail )
    } )

    it( 'update configuration', ( done: DoneFn ) => {
        const { component, fixture, dom } = newComponent( of( new SocialUser() ) )
        httpTestingController.expectOne( '/configuration' ).flush( configuration )
        fixture.detectChanges()
        fixture.whenStable().then(() => {
            setValue( dom, '#googleClientId', 'CHANGE_GOOGLE_CLIENT_ID' )
            setValue( dom, '.googleId', 'CHANGE_GOOGLE_ADMIN_ID' )
            setValue( dom, '.notes', 'CHANGE_MOCK_NOTES' )
            setChecked( dom, '#paypalEnvironmentLive' )
            setValue( dom, '#paypalClientId', 'CHANGE_PAYPAL_CLIENT_ID' )
            setValue( dom, '#paypalClientSecret', 'CHANGE_PAYPAL_CLIENT_SECRET' )
            setValue( dom, '#paypalInvoiceIdTemplate', 'change_template' )
            fixture.detectChanges()
            return fixture.whenStable()
        } ).then(() => {
            component.updateConfiguration()

            // Ensure updated configuration
            const req = httpTestingController.expectOne( '/configuration' )
            expect( req.request.method ).toEqual( 'POST' )
            const update: Configuration = req.request.body
            expect( update.googleClientId ).toEqual( 'CHANGE_GOOGLE_CLIENT_ID' )
            expect( update.administrators[0].googleId ).toEqual( 'CHANGE_GOOGLE_ADMIN_ID' )
            expect( update.administrators[0].notes ).toEqual( 'CHANGE_MOCK_NOTES' )
            expect( update.paypalEnvironment ).toEqual( 'live' )
            expect( update.paypalClientId ).toEqual( 'CHANGE_PAYPAL_CLIENT_ID' )
            expect( update.paypalClientSecret ).toEqual( 'CHANGE_PAYPAL_CLIENT_SECRET' )
            expect( update.paypalInvoiceIdTemplate ).toEqual( 'change_template' )
            done()
        } ).catch( fail )
    } )

} )