import { CheckoutComponent } from './checkout.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'

describe( 'CheckoutComponent', () => {

    let component: CheckoutComponent
    let fixture: ComponentFixture<CheckoutComponent>
    let dom: HTMLElement

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
        dom = fixture.nativeElement
    } )

    afterEach(() => httpTestingController.verify() )

    it( 'should load script', ( done: DoneFn ) => {
        expect( component ).toBeTruthy()
        component.ngOnInit()
        const req = httpTestingController.expectOne( '/initialise' )
        req.flush( {
            paypalClientId: 'MOCK_CLIENT_ID',
            paypalCurrency: 'MOCK_CURRENCY'
        } )

        // Allow HTTP response to be processed
        fixture.detectChanges()
        fixture.whenStable().then(() => {
            // Confirm promise created
            const scriptSrc = 'https://www.paypal.com/sdk/js?client-id=MOCK_CLIENT_ID&currency=MOCK_CURRENCY'
            const scriptPromise = CheckoutComponent.scriptLoadPromises[scriptSrc]
            expect( scriptPromise ).toBeTruthy( 'should load script' )

            // Ensure script being loaded
            fixture.detectChanges()
            const script = document.body.querySelector( 'script:last-child' )
            expect( script ).toBeTruthy( 'should add script' )
            expect( script.attributes['src'].textContent ).toEqual( scriptSrc )

            done()
        } )
    } )

} )
