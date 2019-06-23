import { InitialiseService } from './initialise.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AlertService } from './alert.service'
import { Initialisation } from './server-api.service'
import { Array } from 'core-js'

describe( 'InitialiseService', () => {

    let service: InitialiseService

    let successAlert: string = null
    let errorAlerts: Array<any> = null

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController
    let alertService: AlertService

    beforeEach(() => {
        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )

        alertService = TestBed.get( AlertService )
        successAlert = null
        errorAlerts = []
        alertService.addListener( {
            success: ( message ) => successAlert = message,
            error: ( ex ) => errorAlerts.push( ex )
        } )

        service = TestBed.get( InitialiseService )
    } )

    afterEach(() => {
        httpTestingController.verify()
    } )

    it( 'should be created', () => {
        expect( service ).toBeTruthy()
        httpTestingController.expectOne( "/initialise" )
    } )

    it( 'initialised successfully', ( done: DoneFn ) => {
        const init = { googleClientId: 'MOCK' }
        service.initialisation().subscribe(( initialisation: Initialisation ) => {
            expect( initialisation.googleClientId ).toEqual( 'MOCK' )
            done()
        } )
        const req = httpTestingController.expectOne( "/initialise" )
        expect( req.request.method ).toEqual( "GET" )
        req.flush( init )
    } )

    it( 'failed to initialise', ( done: DoneFn ) => {
        service.initialisation().subscribe(
            () => fail( 'Should not be successful' ),
            ( ex: HttpErrorResponse ) => {
                expect( ex.error ).toEqual( 'Test' )
                expect( errorAlerts[0] ).toEqual( 'Failed to initialise. Please refresh page.' )
                expect( errorAlerts[1] ).toEqual( ex )
                done()
            } )
        const req = httpTestingController.expectOne( "/initialise" )
        expect( req.request.method ).toEqual( "GET" )
        req.flush( 'Test', { status: 500, statusText: 'Error' } )
    } )

} )
