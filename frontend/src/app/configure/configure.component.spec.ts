import { ConfigureComponent } from './configure.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { ReactiveFormsModule } from '@angular/forms'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { of } from 'rxjs'


describe( 'ConfigureComponent', () => {

    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['authenticationState'] )

        TestBed.configureTestingModule( {
            declarations: [ConfigureComponent],
            imports: [HttpClientTestingModule, ReactiveFormsModule],
            providers: [
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newComponent( user: SocialUser = null ): { component: ConfigureComponent, fixture: ComponentFixture<ConfigureComponent> } {
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( ConfigureComponent )
        const component = fixture.componentInstance
        fixture.detectChanges()
        return { component, fixture }
    }

    it( 'should create', () => {
        const { component } = newComponent()
        expect( component ).toBeTruthy()
    } )
} )
