import { MainComponent } from './main.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { ReactiveFormsModule } from '@angular/forms'
import { Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { of } from 'rxjs'
import { Domain } from '../server-api.service'
import * as moment from 'moment'
import { formatDate, parseDate } from '../server-api.service'
import { Array } from 'core-js'

describe( 'MainComponent', () => {

    let authenticationServiceSpy: any
    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['initialise', 'readyState', 'authenticationState'] )

        TestBed.configureTestingModule( {
            declarations: [MainComponent],
            imports: [
                ReactiveFormsModule,
                RouterTestingModule.withRoutes( [{ path: '', component: MainComponent }] ),
                HttpClientTestingModule
            ],
            providers: [
                { provide: AuthenticationService, useValue: authenticationServiceSpy },
            ],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    afterEach(() => httpTestingController.verify() )

    function newComponent( user: SocialUser = new SocialUser() ): { component: MainComponent, fixture: ComponentFixture<MainComponent>, dom: HTMLElement } {
        authenticationServiceSpy.authenticationState.and.returnValue( of( user ) )
        const fixture = TestBed.createComponent( MainComponent )
        const component = fixture.componentInstance
        expect( component ).toBeTruthy()
        const dom = fixture.nativeElement
        fixture.detectChanges()
        return { component, fixture, dom }
    }

    function loadDomains( domains: Array<Domain>, fixture: ComponentFixture<MainComponent> ): Array<Domain> {
        const req = httpTestingController.expectOne( '/domains' )
        expect( req.request.method ).toEqual( 'GET' )
        req.flush( domains )
        fixture.detectChanges()
        return domains
    }

    function newDomain( name: string, expiry: moment.Moment = moment() ): Domain {
        return {
            domainName: name,
            expiresDate: formatDate( expiry )
        }
    }

    function expectDomains( domains: Array<Domain>, component: MainComponent, dom: HTMLElement ) {

        // Ensure correct number of domains
        expect( component.sortedDomains.length ).toEqual( domains.length, 'single domain' )

        // Find the table
        const tableBody = dom.querySelector( 'tbody' )
        expect( tableBody ).toBeTruthy( 'Can not find table body' )

        // Verify domains correctly rendered to screen
        for ( let rowIndex = 0; rowIndex < domains.length; rowIndex++ ) {

            // Confirm row for the domain
            const row = tableBody.childNodes[rowIndex + 1] // +1 avoid #comment
            expect( row ).toBeTruthy( 'Missing row ' + rowIndex + ': ' + row )
            expect( row.nodeName ).toEqual( 'TR', row )

            // Confirm the domain name
            const domainCell = row.childNodes[0]
            expect( domainCell ).toBeTruthy( 'Missing domain name for row ' + rowIndex )
            expect( domainCell.textContent ).toEqual( domains[rowIndex].domainName, 'Incorrect domain for row ' + rowIndex )

            // Confirm the expire date
            const expireCell = row.childNodes[1]
            const expectedExpires = parseDate( domains[rowIndex].expiresDate ).format( 'D MMM YYYY' )
            expect( expireCell ).toBeTruthy( 'Missing expire for row ' + rowIndex )
            expect( expireCell.textContent ).toEqual( expectedExpires, 'Incorrect expiry date for row ' + rowIndex )
        }
    }

    it( 'not logged in', () => {
        const { component } = newComponent( null )
        expect( component.domains ).toEqual( [] )
        expect( component.sortedDomains ).toEqual( [] )
    } )

    it( 'no domains', () => {
        const { component, fixture } = newComponent()
        loadDomains( [], fixture )
        expect( component.sortedDomains ).toEqual( [] )
    } )

    it( 'single domain', () => {
        const { component, fixture, dom } = newComponent()
        const domains = loadDomains( [newDomain( 'officefloor.org' )], fixture )
        expectDomains( domains, component, dom )
    } )

    it( 'multiple domains', () => {
        const { component, fixture, dom } = newComponent()
        const domains = loadDomains( [
            newDomain( 'officefloor.org' ),
            newDomain( 'activicy.com' ),
            newDomain( 'sagenschneider.net' ),
        ], fixture )
        expectDomains( domains, component, dom )
    } )

    it( 'sort domains by name', () => {
        const { component, fixture, dom } = newComponent()
        const domains = loadDomains( [
            newDomain( 'officefloor.org' ),
            newDomain( 'activicy.com' ),
            newDomain( 'sagenschneider.net' ),
        ], fixture )
        component.sortDomains( {
            active: 'domain',
            direction: 'asc'
        } )
        fixture.detectChanges()
        expectDomains( [domains[1], domains[0], domains[2]], component, dom )
    } )

    it( 'sort domains by expiry', () => {
        const { component, fixture, dom } = newComponent()
        const now = moment()
        const domains = loadDomains( [
            newDomain( 'officefloor.org', now.add( 1, 'day' ) ),
            newDomain( 'activicy.com', now.add( 2, 'year' ) ),
            newDomain( 'sagenschneider.net', now.subtract( 10, 'year' ) ),
        ], fixture )
        component.sortDomains( {
            active: 'expireDate',
            direction: 'asc'
        } )
        fixture.detectChanges()
        expectDomains( [domains[2], domains[0], domains[1]], component, dom )
    } )

} )
