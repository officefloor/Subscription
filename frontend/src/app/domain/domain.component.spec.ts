import { DomainComponent } from './domain.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { CheckoutComponent } from '../checkout/checkout.component'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { SocialUser } from "angularx-social-login"
import { of } from 'rxjs'
import { AuthenticationService } from '../authentication.service'
import { ActivatedRoute } from '@angular/router'

describe( 'DomainComponent', () => {

    let authenticationServiceSpy: any
    let activatedRoute: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['authenticationState'] )
        activatedRoute = { snapshot: { paramMap: { get: () => 'officefloor.org' } } }

        TestBed.configureTestingModule( {
            declarations: [DomainComponent, CheckoutComponent],
            imports: [HttpClientTestingModule],
            providers: [
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
                { provide: ActivatedRoute, useValue: activatedRoute },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    function newComponent( user: SocialUser = null ): { component: DomainComponent, fixture: ComponentFixture<DomainComponent> } {
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( DomainComponent )
        const component = fixture.componentInstance
        fixture.detectChanges()
        return { component, fixture }
    }

    it( 'should create', () => {
        const { component } = newComponent()
        expect( component ).toBeTruthy()
    } )

} )
