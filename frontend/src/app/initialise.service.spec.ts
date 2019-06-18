import { InitialiseService } from './initialise.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'


describe( 'InitialiseService', () => {

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach(() => {
        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } )

    it( 'should be created', () => {
        const service: InitialiseService = TestBed.get( InitialiseService )
        expect( service ).toBeTruthy()
    } )
} )
