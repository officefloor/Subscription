import { InitialiseService } from './initialise.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AlertService } from './alert.service'
import { Initialisation } from './server-api.service'

describe( 'InitialiseService', () => {

    let service: InitialiseService

    let success: string = null
    let error: any = null

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
        success = null
        error = null
        alertService.addListener( {
            success: ( message ) => success = message,
            error: ( ex ) => error = ex
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
        service.initialisation().then(( initialisation: Initialisation ) => {
            expect( initialisation.googleClientId ).toEqual( 'MOCK' )
            done()
        } )
        const req = httpTestingController.expectOne( "/initialise" )
        expect( req.request.method ).toEqual( "GET" )
        req.flush( init )
    } )

    it( 'failed to initialise', ( done: DoneFn ) => {
        service.initialisation().catch(( ex: HttpErrorResponse ) => {
            expect( ex.error ).toEqual( 'Test' )
            done()
        } )
        const req = httpTestingController.expectOne( "/initialise" )
        expect( req.request.method ).toEqual( "GET" )
        req.flush( 'Test', { status: 500, statusText: 'Error' } )
    } )

} )
