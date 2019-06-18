import { AppComponent } from './app.component'
import { TestBed, async, ComponentFixture } from '@angular/core/testing'
import { of } from 'rxjs'
import { RouterTestingModule } from '@angular/router/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { LoginComponent } from './login/login.component'
import { AlertComponent } from './alert/alert.component'
import { AuthenticationService } from './authentication.service'
import { SocialUser } from 'angularx-social-login'

describe( 'AppComponent', () => {

    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['initialise', 'readyState', 'authenticationState'] )

        TestBed.configureTestingModule( {
            imports: [RouterTestingModule, HttpClientTestingModule],
            declarations: [AppComponent, LoginComponent, AlertComponent],
            providers: [{ provide: AuthenticationService, useValue: authenticationServiceSpy }],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newApp( isReady: boolean = true, user: SocialUser = null ): { app: AppComponent, fixture: ComponentFixture<AppComponent> } {
        authenticationServiceSpy.readyState.and.returnValue( of( isReady ) )
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        authenticationServiceSpy.initialise
        const fixture = TestBed.createComponent( AppComponent )
        const app = fixture.debugElement.componentInstance
        fixture.detectChanges()
        return { app, fixture }
    }

    it( `should be ready`, () => {
        const { app } = newApp()
        expect( app.isInitialised ).toBeTruthy()
    } )

    it( 'should render title in a h1 tag', () => {
        const { app, fixture } = newApp()
        const compiled = fixture.debugElement.nativeElement
        expect( compiled.querySelector( 'h1' ).textContent ).toContain( 'OfficeFloor Subscription' )
    } )

} )