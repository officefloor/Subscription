import { async, ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { AlertComponent } from './alert.component'
import { AlertService } from '../alert.service'
import { HttpErrorResponse } from '@angular/common/http'
import { Error } from 'core-js'

describe( 'AlertComponent', () => {

    let component: AlertComponent
    let fixture: ComponentFixture<AlertComponent>
    let dom: HTMLElement

    let alertService: AlertService

    beforeEach( async(() => {
        TestBed.configureTestingModule( {
            declarations: [AlertComponent]
        } ).compileComponents()
    } ) )

    beforeEach(() => {
        fixture = TestBed.createComponent( AlertComponent )
        fixture.detectChanges()
        component = fixture.componentInstance
        dom = fixture.nativeElement
        alertService = TestBed.get( AlertService )
    } )

    it( 'success alert', () => {
        expect( component ).toBeTruthy()
        alertService.success( 'SUCCESS' )
        fixture.detectChanges()
        expect( component.alerts.length ).toEqual( 1, 'should register alert' )
        expect( dom.querySelector( '.alert-content' ).textContent ).toEqual( 'SUCCESS' )
    } )

    it( 'success alert (auto disappear)', fakeAsync(() => {
        alertService.success( 'SUCCESS' )
        fixture.detectChanges()
        expect( dom.querySelector( '.alert-content' ).textContent ).toEqual( 'SUCCESS' )
        tick( 10 * 1000 )
        fixture.detectChanges()
        expect( dom.querySelector( '.alert-content' ) ).toBeNull( 'should clear success' )
    } ) )

    it( 'unregister from alerts', () => {
        component.ngOnDestroy()
        alertService.success( 'SUCCESS' )
        fixture.detectChanges()
        expect( dom.querySelector( '.alert-content' ) ).toBeNull( 'should not display as unregistered' )
    } )

    // Run error test for each error type
    for ( let test of [
        { message: 'text', error: 'text' },
        { message: 'Session expired. Please logout and log back in.', error: new HttpErrorResponse( { status: 401 } ) },
        { message: 'Sorry you do not have permissions', error: new HttpErrorResponse( { status: 403 } ) },
        { message: 'Failure communicating to server. Please refresh page and try again.', error: new HttpErrorResponse( { status: 422 } ) },
        { message: 'Technical failure. Please reload page and retry. If error continues please raise support ticket', error: new HttpErrorResponse( { status: 500 } ), name: 'Server error' },
        { message: 'Error message', error: new Error( 'Error message' ) },
        { message: 'Technical failure. Please reload page and retry. If error continues please raise support ticket', error: {}, name: 'Unknown error' },
    ] ) {
        it( 'error: ' + ( test.name ? test.name : test.message ), () => {
            alertService.error( test.error )
            fixture.detectChanges()
            expect( dom.querySelector( '.alert-content' ).textContent ).toEqual( test.message )
        } )
    }

} )