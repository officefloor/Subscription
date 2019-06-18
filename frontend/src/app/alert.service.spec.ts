import { AlertService } from './alert.service'
import { TestBed } from '@angular/core/testing'


describe( 'AlertService', () => {
    beforeEach(() => TestBed.configureTestingModule( {} ) )

    it( 'should be created', () => {
        const service: AlertService = TestBed.get( AlertService )
        expect( service ).toBeTruthy()
    } )
} )
