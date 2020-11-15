import { RegisterComponent } from './register.component'
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing'
import { ReactiveFormsModule } from '@angular/forms'
import { Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { expectValue, expectText, setValue } from '../app.component.spec'

describe( 'RegisterComponent', () => {

    let component: RegisterComponent
    let fixture: ComponentFixture<RegisterComponent>
    let dom: HTMLElement

    beforeEach( waitForAsync(() => {
        TestBed.configureTestingModule( {
            declarations: [RegisterComponent],
            imports: [
                ReactiveFormsModule,
                RouterTestingModule.withRoutes( [{ path: '', component: RegisterComponent }] ),
            ]
        } ).compileComponents()
    } ) )

    beforeEach(() => {
        fixture = TestBed.createComponent( RegisterComponent )
        component = fixture.componentInstance
        dom = fixture.nativeElement
        fixture.detectChanges()
    } )

    it( 'should initiate with no error', () => {
        expect( component ).toBeTruthy()
        expectValue( dom, '#domainName', '' )
        expectText( dom, '.invalid-feedback', '' )
    } )

    for ( let test of [
        { domain: 'contains spaces', error: 'May not contain spaces' },
        { domain: ' .startsWithDot', error: "May not start with '.'" },
        { domain: ' endsWithDot. ', error: "May not end with '.'" },
        { domain: 'wrong', error: "Must contain at least one '.' (e.g. domain.com)" },
        { domain: 'valid.org', error: '', name: 'valid domain' },
    ] ) {
        it( test.name ? test.name : 'trigger error: ' + test.error, ( done: DoneFn ) => {
            setValue( dom, '#domainName', test.domain )
            fixture.detectChanges()
            fixture.whenStable().then(() => {
                expectText( dom, '.invalid-feedback', test.error )
                done()
            } )
        } )
    }

} )