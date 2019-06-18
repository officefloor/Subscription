import { CheckoutComponent } from './checkout.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'

describe( 'CheckoutComponent', () => {

    let component: CheckoutComponent
    let fixture: ComponentFixture<CheckoutComponent>

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController

    beforeEach( async(() => {
        TestBed.configureTestingModule( {
            declarations: [CheckoutComponent],
            imports: [HttpClientTestingModule],
        } ).compileComponents()

        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
    } ) )

    beforeEach(() => {
        fixture = TestBed.createComponent( CheckoutComponent )
        component = fixture.componentInstance
        fixture.detectChanges()
    } )

    it( 'should create', () => {
        expect( component ).toBeTruthy()
    } )

} )
