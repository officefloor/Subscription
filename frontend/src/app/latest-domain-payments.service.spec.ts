import { LatestDomainPaymentsService } from './latest-domain-payments.service'
import { TestBed } from '@angular/core/testing'


describe( 'LatestDomainPaymentsService', () => {
    beforeEach(() => TestBed.configureTestingModule( {} ) )

    it( 'should be created', () => {
        const service: LatestDomainPaymentsService = TestBed.get( LatestDomainPaymentsService )
        expect( service ).toBeTruthy()
    } )
} )
