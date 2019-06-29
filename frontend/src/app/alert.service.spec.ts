import { AlertService, AlertListener } from './alert.service'
import { TestBed } from '@angular/core/testing'
import { Observable, BehaviorSubject, of } from 'rxjs'

describe( 'AlertService', () => {

    let service: AlertService

    let success: string = null
    let error: any = null

    let observable: BehaviorSubject<string>

    beforeEach(() => {
        TestBed.configureTestingModule( {} )
        service = TestBed.get( AlertService )

        // Reset for listening in next test
        observable = new BehaviorSubject<string>( null )
        success = null
        error = null
        service.addListener( {
            success: ( message ) => success = message,
            error: ( ex ) => error = ex,
        } )
    } )

    it( 'should be created', () => {
        expect( service ).toBeTruthy()
    } )

    it( 'notify success', () => {
        const message = 'SUCCESS'
        service.success( message )
        expect( success ).toEqual( message )
    } )

    it( 'notify error', () => {
        const ex = new Error( 'ERROR' )
        service.error( ex )
        expect( error ).toEqual( ex )
    } )

    it( 'alert', ( done: DoneFn ) => {
        const ex = new Error( 'ERROR' )
        observable.error( ex )
        observable.pipe(
            service.alertError()
        ).subscribe(() => fail( 'Should not be successful' ), () => {
            expect( error ).toEqual( ex )
            done()
        } )
    } )

    it( 'no alert', ( done: DoneFn ) => {
        observable.next( 'SUCCESS' )
        observable.pipe(
            service.alertError()
        ).subscribe(() => {
            expect( error ).toBeNull( 'Should be no error' )
            done()
        } )
    } )

    it( 'alert, not include', ( done: DoneFn ) => {
        observable.error( 'ERROR' )
        observable.pipe(
            service.alertError(( ex ) => ex !== 'ERROR' )
        ).subscribe(() => fail( 'Should not be successful' ), () => {
            expect( error ).toBeNull( 'Should filter out error' )
            done()
        } )
    } )

} )