import { RegisterComponent } from './register.component'
import { async, ComponentFixture, TestBed } from '@angular/core/testing'
import { ReactiveFormsModule } from '@angular/forms'
import { Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'

describe( 'RegisterComponent', () => {
    let component: RegisterComponent
    let fixture: ComponentFixture<RegisterComponent>

    beforeEach( async(() => {
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
        fixture.detectChanges()
    } )

    it( 'should create', () => {
        expect( component ).toBeTruthy()
    } )

} )