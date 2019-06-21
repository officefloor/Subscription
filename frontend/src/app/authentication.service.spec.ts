import { AuthenticationService } from './authentication.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AuthService, SocialUser } from 'angularx-social-login'
import { InitialiseService } from './initialise.service'
import { Initialisation } from './server-api.service'
//import { Promise } from 'core-js'
import { of, BehaviorSubject } from 'rxjs'

describe( 'AuthenticationService', () => {

    let service: AuthenticationService

    let initialisationServiceSpy: any
    let authServiceMock: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController
    let readyState = new BehaviorSubject<[string]>( null )
    let authState = new BehaviorSubject<SocialUser>( null )

    beforeEach(() => {
        initialisationServiceSpy = jasmine.createSpyObj( 'InitialiseService', ['initialisation'] )
        authServiceMock = {
            readyState: readyState,
            authState: authState
        }

        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
            providers: [
                { provide: InitialiseService, useValue: initialisationServiceSpy },
                { provide: AuthService, useValue: authServiceMock },
            ]
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )

        service = TestBed.get( AuthenticationService )
    } )

    function newInitialisation( isAuthenticationRequired: boolean = true ): Initialisation {
        return {
            isAuthenticationRequired: isAuthenticationRequired,
            googleClientId: 'MOCK_GOOGLE_CLIENT_ID',
            paypalClientId: 'MOCK_PAYPAL_CLIENT_ID',
            paypalCurrency: 'MOCK_PAYPAL_CURRENCY',
        }
    }

    it( 'should create', () => expect( service ).toBeTruthy() )

    it( 'no authentication required', ( done: DoneFn ) => {
        const initialisation = newInitialisation( false )
        initialisationServiceSpy.initialisation.and.returnValue( Promise.resolve( initialisation ) )
        service.initialise().then(( result: Initialisation ) => {
            expect( result ).toEqual( initialisation )
            service.readyState().subscribe(( isReady ) => expect( isReady ).toBeTruthy( 'Should be ready' ) )
            service.authenticationState().subscribe(( user ) => {
                expect( user ).toBeTruthy()
                expect( user.name ).toEqual( 'No Authentication' )
                done()
            } )
        } ).catch( fail )
    } )

    it( 'should be ready', ( done: DoneFn ) => {
        const initialisation = newInitialisation()
        initialisationServiceSpy.initialisation.and.returnValue( Promise.resolve( initialisation ) )
        readyState.next( ['GOOGLE'] )
        service.initialise().then(( result: Initialisation ) => {
            expect( result ).toEqual( initialisation )
            service.readyState().subscribe(( isReady ) => {
                expect( isReady ).toBeTruthy( 'Should be ready' )
                done()
            } )
        } ).catch( fail )
    } )

    it( 'not authenticated', ( done: DoneFn ) => {
        initialisationServiceSpy.initialisation.and.returnValue( Promise.resolve( newInitialisation() ) )
        readyState.next( ['GOOGLE'] )
        service.initialise().then(( result: Initialisation ) => {
            service.authenticationState().subscribe(( user ) => {
                expect( user ).toBeNull( 'Should not be logged in' )
                done()
            } )
        } ).catch( fail )
    } )

    it( 'authenticated', ( done: DoneFn ) => {

        // TODO finish
        if ( true == true ) return done()

        const user = new SocialUser()
        initialisationServiceSpy.initialisation.and.returnValue( Promise.resolve( newInitialisation() ) )
        readyState.next( ['GOOGLE'] )
        authState.next( user )
        service.initialise().then(( result: Initialisation ) => {
            service.authenticationState().subscribe(( authUser ) => {
                expect( authUser ).toEqual( user, 'Should be logged in' )
                done()
            } )
        } ).catch( fail )
    } )

    it( 'authentication error', ( done: DoneFn ) => {

        // TODO finish
        if ( true == true ) return done()

        fail( new Error( 'TODO implement' ) )
    } )

} )
