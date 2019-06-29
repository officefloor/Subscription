import { ServerApiService } from './server-api.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'


describe( 'ServerApiService', () => {

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach(() => {
        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } )

    it( 'should be created (tested by other tests)', () => {
        const service: ServerApiService = TestBed.get( ServerApiService )
        expect( service ).toBeTruthy()
    } )
} )
