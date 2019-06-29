import { environment } from '../environments/environment.prod'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { TestBed } from '@angular/core/testing'
import { ServerApiService, DomainPayments, formatDate } from '../app/server-api.service'
import * as moment from 'moment'

describe( 'environment.prod', () => {

    let httpClient: HttpClient
    let httpTestingController: HttpTestingController
    let serverApiService: ServerApiService

    beforeEach(() => {
        TestBed.configureTestingModule( {
            imports: [HttpClientTestingModule],
        } )
        httpClient = TestBed.get( HttpClient )
        httpTestingController = TestBed.get( HttpTestingController )
        serverApiService = TestBed.get( ServerApiService )
    } )

    afterEach(() => httpTestingController.verify() )

    it( 'create order', ( done: DoneFn ) => {
        environment.createOrder( 'officefloor.org', false, 'AUD', serverApiService, null, null ).then(( result ) => {
            expect( result ).toEqual( 'MOCK_ORDER_ID' )
            done()
        } ).catch( fail )
        const req = httpTestingController.expectOne( '/invoices/domain/officefloor.org?restart=false' )
        expect( req.request.method ).toEqual( 'POST' )
        req.flush( {
            orderId: 'MOCK_ORDER_ID'
        } )
    } )

    it( 'capture payment', ( done: DoneFn ) => {
        const domainPayments: DomainPayments = {
            domainName: 'officefloor.org',
            expiresDate: formatDate( moment().add( 1, 'year' ) ),
            payments: []
        }
        environment.capturePayment( 'MOCK_ORDER_ID', serverApiService, null, null ).then(( result ) => {
            expect( result ).toEqual( domainPayments )
            done()
        } )
        const req = httpTestingController.expectOne( '/payments/domain/MOCK_ORDER_ID' )
        expect( req.request.method ).toEqual( 'POST' )
        req.flush( domainPayments )
    } )

})