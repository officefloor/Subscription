import { AppComponent } from './app.component'
import { TestBed, ComponentFixture, waitForAsync } from '@angular/core/testing'
import { of } from 'rxjs'
import { RouterTestingModule } from '@angular/router/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { LoginComponent } from './login/login.component'
import { AlertComponent } from './alert/alert.component'
import { AuthenticationService } from './authentication.service'
import { SocialUser } from 'angularx-social-login'

export function expectVisible( dom: HTMLElement, selector: string, isVisible: boolean ): void {
    const element = dom.querySelector( selector )
    expect( element ).toBeTruthy( 'Can not find element ' + selector )
    expect( element.hasAttribute( 'hidden' ) ).toEqual( !isVisible, ( isVisible ? 'Visible ' : 'Hidden ' ) + selector )
}

export function expectValue( dom: HTMLElement, selector: string, value: string ) {
    const node = dom.querySelector( selector )
    expect( node instanceof HTMLInputElement ).toEqual( true )
    const input: HTMLInputElement = node as HTMLInputElement
    expect( input.value ).toEqual( value, 'wrong value for ' + selector )
}

export function expectChecked( dom: HTMLElement, selector: string, checked: boolean ) {
    const node = dom.querySelector( selector )
    expect( node instanceof HTMLInputElement ).toEqual( true )
    const input: HTMLInputElement = node as HTMLInputElement
    expect( input.checked ).toEqual( checked, 'incorrect checked' )
}

export function expectText( dom: HTMLElement, selector: string, value: string ) {
    const node = dom.querySelector( selector )
    expect( node ).toBeTruthy()
    expect( node.textContent ).toEqual( value, 'wrong value for ' + selector )
}

export function setValue( dom: HTMLElement, selector: string, value: string ) {
    const node = dom.querySelector( selector )
    expect( node instanceof HTMLInputElement ).toEqual( true )
    const input: HTMLInputElement = node as HTMLInputElement
    input.value = value
    input.dispatchEvent( new Event( 'input' ) )
}

export function setChecked( dom: HTMLElement, selector: string ) {
    const node = dom.querySelector( selector )
    expect( node instanceof HTMLInputElement ).toEqual( true )
    const input: HTMLInputElement = node as HTMLInputElement
    input.dispatchEvent( new Event( 'change' ) )
}


describe( 'AppComponent', () => {

    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( waitForAsync(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['initialise', 'readyState', 'authenticationState'] )

        TestBed.configureTestingModule( {
            imports: [RouterTestingModule, HttpClientTestingModule],
            declarations: [AppComponent, LoginComponent, AlertComponent],
            providers: [{ provide: AuthenticationService, useValue: authenticationServiceSpy }],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newApp( isReady: boolean = true, user: SocialUser = null ): { app: AppComponent, fixture: ComponentFixture<AppComponent>, dom: HTMLElement } {
        authenticationServiceSpy.readyState.and.returnValue( of( isReady ) )
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        authenticationServiceSpy.initialise.and.returnValue( of( 'initialised' ) )
        const fixture = TestBed.createComponent( AppComponent )
        const app = fixture.debugElement.componentInstance
        const dom = fixture.debugElement.nativeElement
        fixture.detectChanges()
        return { app, fixture, dom }
    }

    it( 'show loading', () => {
        const { dom } = newApp( false )
        expectVisible( dom, '.loading', true )
        expectVisible( dom, '.login', false )
        expectVisible( dom, '.ready', false )
    } )

    it( 'not logged in', () => {
        const { dom } = newApp( true, null )
        expectVisible( dom, '.loading', false )
        expectVisible( dom, '.login', true )
        expectVisible( dom, '.ready', true )
        expectVisible( dom, '.content', false )
        expectVisible( dom, '.login-content', true )
    } )

    it( 'logged in', () => {
        const { dom } = newApp( true, new SocialUser() )
        expectVisible( dom, '.loading', false )
        expectVisible( dom, '.ready', true )
        expectVisible( dom, '.content', true )
        expectVisible( dom, '.login-content', false )
    } )

} )