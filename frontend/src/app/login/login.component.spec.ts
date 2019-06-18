import { LoginComponent } from './login.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from 'angularx-social-login'
import { InitialiseService } from '../initialise.service'
import { of } from 'rxjs'

describe( 'LoginComponent', () => {

    let initialiseServiceSpy: any
    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        initialiseServiceSpy = jasmine.createSpyObj( 'InitialiseService', ['intialisation'] )
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['authenticationState'] )

        TestBed.configureTestingModule( {
            declarations: [LoginComponent],
            imports: [HttpClientTestingModule],
            providers: [
                { provide: InitialiseService, useValue: initialiseServiceSpy },
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newComponent( user: SocialUser = null ): { component: LoginComponent, fixture: ComponentFixture<LoginComponent> } {
        initialiseServiceSpy.intialisation.and.returnValue( Promise.resolve( { isAuthenticationRequired: true } ) )
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( LoginComponent )
        const component = fixture.componentInstance
        fixture.detectChanges()
        return { component, fixture }
    }

    it( 'should create', () => {
        const { component } = newComponent()
        expect( component ).toBeTruthy()
    } )
} )
