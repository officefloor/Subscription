import { MainComponent } from './main.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { ReactiveFormsModule } from '@angular/forms'
import { Router, RouterModule } from '@angular/router'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { of } from 'rxjs'

describe( 'MainComponent', () => {

    let authenticationServiceSpy: any
    let routerSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['initialise', 'readyState', 'authenticationState'] )
        routerSpy = jasmine.createSpyObj( 'Router', ['navigate'] )

        TestBed.configureTestingModule( {
            declarations: [MainComponent],
            imports: [ReactiveFormsModule, RouterModule, HttpClientTestingModule],
            providers: [
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
                { provide: Router, useValue: routerSpy },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newComponent( user: SocialUser = null ): { component: MainComponent, fixture: ComponentFixture<MainComponent> } {
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( MainComponent )
        const component = fixture.componentInstance
        fixture.detectChanges()
        return { component, fixture }
    }

    it( 'should create', () => {
        const component = newComponent()
        expect( component ).toBeTruthy()
    } )
} )
