import { ServerApiService } from './server-api.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'


describe( 'ServerApiService', () => {

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController
    let service: ServerApiService

    beforeEach(() => {
        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )

        service = TestBed.get( ServerApiService )
    } )

    it( 'should be created (tested by other tests)', () => {
        expect( service ).toBeTruthy()
    } )

    it( 'Check relative url', () => {
        expect( service.isServerUrl( '/relative/url' ) ).toEqual( true )
    } )

    it( 'Check server url', () => {
        expect( service.isServerUrl( window.location.href ) ).toEqual( true )
    } )

    it( 'Check non-server url', () => {
        expect( service.isServerUrl( 'http://not.server' ) ).toEqual( false )
    } )

} )
