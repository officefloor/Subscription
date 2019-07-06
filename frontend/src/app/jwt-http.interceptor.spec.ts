import { JwtHttpInterceptor } from './jwt-http.interceptor'
import { TestBed } from '@angular/core/testing'
import { Injector } from '@angular/core';
import { of, throwError } from 'rxjs'
import { map } from 'rxjs/operators'
import { HttpResponse, HttpErrorResponse } from '@angular/common/http'
import { AccessTokenResponse, ServerApiService } from './server-api.service'
import { AuthenticationService } from './authentication.service'

describe( 'JwtHttpInterceptor', () => {

    let interceptor: JwtHttpInterceptor

    let injectorSpy: any
    let requestSpy: any
    let nextSpy: any
    let authenticationServiceSpy: any
    let serverApiService: any

    const injectServices = ( type ) => {
        if ( type === AuthenticationService ) {
            return authenticationServiceSpy
        } else if ( type === ServerApiService ) {
            return serverApiService
        } else {
            throw new Error( 'Invalid injection type: ' + type )
        }
    }

    beforeEach(() => {
        injectorSpy = jasmine.createSpyObj( 'Injector', ['get'] )
        requestSpy = jasmine.createSpyObj( 'HttpRequest', ['clone'] )
        requestSpy.url = '/test'
        nextSpy = jasmine.createSpyObj( 'HttpHandler', ['handle'] )
        authenticationServiceSpy = jasmine.createSpyObj( 'AuthenticationService', ['getAccessToken', 'refreshAccessToken', 'signOut'] )
        serverApiService = jasmine.createSpyObj( 'ServerApiService', ['isServerUrl'] )

        // Create interceptor to test
        interceptor = new JwtHttpInterceptor( injectorSpy )
    } )

    it( 'successful request (no authentication)', ( done: DoneFn ) => {
        const response: any = { with: 'No Authorization' }
        injectorSpy.get.and.returnValue( null )
        nextSpy.handle.and.returnValue( of( response ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(( event ) => {
            expect( event ).toEqual( response, 'should be successful request' )
            done()
        }, fail )
    } )

    it( 'successful request (with authentication)', ( done: DoneFn ) => {
        const response = new HttpResponse( { status: 200 } )
        const enhancedResponse: any = { with: 'Authorization' }
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( 'MOCK_ACCESS_TOKEN' )
        serverApiService.isServerUrl.and.returnValue( true )
        requestSpy.clone.and.returnValue( enhancedResponse )
        nextSpy.handle.and.returnValue( of( response ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(( event ) => {
            expect( requestSpy.clone.calls.mostRecent().args[0].setHeaders.Authorization ).toEqual( 'Bearer MOCK_ACCESS_TOKEN' )
            expect( event ).toEqual( response, 'should be successful request' )
            done()
        }, fail )
    } )

    it( 'only authenticate server requests', ( done: DoneFn ) => {
        const response = new HttpResponse( { status: 200 } )
        const enhancedResponse: any = { with: 'Authorization' }
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( 'MOCK_ACCESS_TOKEN' )
        serverApiService.isServerUrl.and.returnValue( false )
        nextSpy.handle.and.returnValue( of( response ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(( event ) => {
            expect( event ).toEqual( response, 'should be successful request' )
            done()
        }, fail )
    } )

    it( 'failed request', ( done: DoneFn ) => {
        const errorResponse = new HttpErrorResponse( { status: 404 } )
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( 'MOCK_ACCESS_TOKEN' )
        serverApiService.isServerUrl.and.returnValue( true )
        requestSpy.clone.and.returnValue( requestSpy )
        nextSpy.handle.and.returnValue( throwError( errorResponse ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(
            fail,
            ( error ) => {
                expect( error ).toBeTruthy( 'Should have error' )
                expect( error.status ).toEqual( 404 )
                done()
            } )
    } )

    const failAuthentication = ( url: string ) => ( done: DoneFn ) => {
        requestSpy.url = url
        const errorResponse = new HttpErrorResponse( { status: 401 } )
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( 'MOCK_ACCESS_TOKEN' )
        serverApiService.isServerUrl.and.returnValue( true )
        requestSpy.clone.and.returnValue( requestSpy )
        nextSpy.handle.and.returnValue( throwError( errorResponse ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(
            fail,
            ( error ) => {
                expect( error instanceof HttpErrorResponse ).toEqual( true, 'incorrect error ' + error )
                expect( error.status ).toEqual( 401, 'Should propagate error' )
                done()
            } )
    }
    it( 'failed authentication /authenticate', failAuthentication( '/authenticate' ) )
    it( 'failed authentication /refreshAccessToken', failAuthentication( '/refreshAccessToken' ) )

    it( 'refresh authentication', ( done: DoneFn ) => {
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( null )
        serverApiService.isServerUrl.and.returnValue( true )
        nextSpy.handle.and.returnValue( throwError( new HttpErrorResponse( { status: 401 } ) ) )
        const successResponse = new HttpResponse( { status: 200 } )
        authenticationServiceSpy.refreshAccessToken.and.returnValue( of( {
            accessToken: 'MOCK_ACCESS_TOKEN'
        } ).pipe( map(( token ) => {
            requestSpy.clone.and.returnValue( requestSpy )
            nextSpy.handle.and.returnValue( of( successResponse ) )
            return token
        } ) ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(
            ( response ) => {
                expect( response ).toEqual( successResponse, 'should be success response' )
                done()
            }, fail )
    } )

    it( 'fail refresh token', ( done: DoneFn ) => {
        injectorSpy.get.and.callFake( injectServices )
        authenticationServiceSpy.getAccessToken.and.returnValue( null )
        serverApiService.isServerUrl.and.returnValue( true )
        const requestFailure = new HttpErrorResponse( { status: 401, url: '/test' } )
        nextSpy.handle.and.returnValue( throwError( requestFailure ) )
        authenticationServiceSpy.refreshAccessToken.and.returnValue( throwError( new HttpErrorResponse( { status: 401, url: '/auth' } ) ) )
        interceptor.intercept( requestSpy, nextSpy ).subscribe(
            fail,
            ( error ) => {
                expect( error ).toEqual( requestFailure, 'should auth failure' )
                expect( authenticationServiceSpy.signOut.calls.count() ).toEqual( 1, 'should trigger logout' )
                done()
            } )
    } )

})