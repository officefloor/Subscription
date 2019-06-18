import { AuthenticationService } from './authentication.service'
import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AuthService, SocialUser } from 'angularx-social-login'

describe( 'AuthenticationService', () => {

    let authServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach(() => {
        authServiceSpy = jasmine.createSpyObj( 'AuthService', [''] )

        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
            providers: [{ provide: AuthService, useValue: authServiceSpy }]
        } )

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } )

    it( 'should be created', () => {
        const service: AuthenticationService = TestBed.get( AuthenticationService )
        expect( service ).toBeTruthy()
    } )
} )
