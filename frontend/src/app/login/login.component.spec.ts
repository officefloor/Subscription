import { LoginComponent } from './login.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from 'angularx-social-login'
import { InitialiseService } from '../initialise.service'
import { of } from 'rxjs'
import { expectVisible } from '../app.component.spec'

describe( 'LoginComponent', () => {

    let initialiseServiceSpy: any
    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        initialiseServiceSpy = jasmine.createSpyObj( 'InitialiseService', ['initialisation'] )
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

    function newComponent( user: SocialUser = null, isAuthenticationRequired: boolean = true ): { component: LoginComponent, fixture: ComponentFixture<LoginComponent>, dom: HTMLElement } {
        initialiseServiceSpy.initialisation.and.returnValue( of( { isAuthenticationRequired: isAuthenticationRequired } ) )
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( LoginComponent )
        const component = fixture.componentInstance
        const dom = fixture.nativeElement
        fixture.detectChanges()
        return { component, fixture, dom }
    }

    it( 'authentication not required', () => {
        const { component, dom } = newComponent( null, false )
        expect( component ).toBeTruthy( 'should create component' )
        expect( component.isAuthenticationRequired ).toBeFalsy( 'should not require authentication' )
        expect( component.user ).toBeNull( 'Should not have user' )
        expectVisible( dom, '.signin', false )
        expectVisible( dom, '.signout', false )
    } )

    it( 'not authenticated', () => {
        const { component, dom } = newComponent( null )
        expect( component.isAuthenticationRequired ).toBeTruthy( 'requires authentication' )
        expect( component.user ).toBeNull( 'Should not have user' )
        expectVisible( dom, '.signin', true )
        expectVisible( dom, '.signout', false )
    } )

    it( 'authenticated (no photo)', () => {
        const user = new SocialUser()
        const { component, dom } = newComponent( user )
        expect( component.user ).toEqual( user, 'Should have user' )
        expectVisible( dom, '.signin', false )
        expectVisible( dom, '.signout', true )
        expect( dom.querySelector( 'img' ).attributes['src'].textContent ).toEqual( '' )
    } )

    it( 'authenticated (with photo)', () => {
        const photoUrl = 'http://officefloor.org/photo.png'
        const user = new SocialUser()
        user.photoUrl = photoUrl
        const { component, dom } = newComponent( user )
        expect( component.user ).toEqual( user, 'Should have user' )
        expectVisible( dom, '.signin', false )
        expectVisible( dom, '.signout', true )
        expect( dom.querySelector( 'img' ).attributes['src'].textContent ).toEqual( photoUrl )
    } )

} )