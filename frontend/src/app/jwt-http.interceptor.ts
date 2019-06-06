import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpResponse, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, switchMap, filter, take, finalize, catchError } from 'rxjs/operators';
import { AuthenticationService } from './authentication.service';
import { AccessTokenResponse } from './server-api.service';

declare let JSON: any

@Injectable()
export class JwtHttpInterceptor implements HttpInterceptor {

    // Enable only one refresh access token request
    private refreshedAccessToken: BehaviorSubject<string> = null;

    constructor(
        private injector: Injector,
    ) { }

    intercept( req: HttpRequest<any>, next: HttpHandler ): Observable<HttpEvent<any>> {

        // Create request with JWT
        const jwtRequest: ( req: HttpRequest<any>, accessToken: string ) => HttpRequest<any> = ( req: HttpRequest<any>, accessToken: string ) => {
            if ( accessToken ) {
                return req.clone( {
                    setHeaders: {
                        Authorization: `Bearer ${accessToken}`
                    }
                } )
            } else {
                return req
            }
        }

        // Create HTTP request/response log
        const logInteraction = ( logger: ( message: string ) => void, request: HttpRequest<any>, resultType: string, result: any ) => {
            logger( 'HttpRequest (Authorization: ' + request.headers.get( 'Authorization' ) + '): ' + JSON.stringify( request, null, 2 ) + "\n\n" + resultType + ': ' + JSON.stringify( result, null, 2 ) )
        }

        // Obtain the authentication service
        const authenticationService: AuthenticationService = this.injector.get( AuthenticationService )

        // Undertake the request
        let authRequest = jwtRequest( req, authenticationService ? authenticationService.getAccessToken() : null )
        return next.handle( authRequest ).pipe(

            // Handle failures
            catchError(( error ) => {

                // Log failure
                logInteraction( console.error, authRequest, 'Error', error )

                // Handle possible failure
                if ( error instanceof HttpErrorResponse ) {
                    const response: HttpErrorResponse = error;

                    // Determine if expired access token
                    if ( response.status === 401 ) {

                        // Do not refresh if authenticate/refresh access token requests
                        if ( req.url.includes( '/authenticate' ) || req.url.includes( '/refreshAccessToken' ) ) {
                            return throwError( error )
                        }

                        // Determine if refreshing access token
                        if ( !this.refreshedAccessToken ) {

                            // Create the refresh to block multiple requests
                            this.refreshedAccessToken = new BehaviorSubject<string>( null )

                            // Attempt to refresh access token
                            let isRefreshed: boolean = false;
                            authenticationService.refreshAccessToken().pipe(
                                map(( refreshResponse: AccessTokenResponse ) => {

                                    // Notify of refreshed access token
                                    if ( refreshResponse.accessToken ) {
                                        this.refreshedAccessToken.next( refreshResponse.accessToken )
                                        isRefreshed = true
                                    }

                                    return refreshResponse
                                } ),
                                finalize(() => {
                                    // Clear to allow refresh again
                                    // Note; if failure then existing requests are not made
                                    this.refreshedAccessToken = null;

                                    // Determine if refreshed
                                    if ( !isRefreshed ) {
                                        // Not refreshed, so log out
                                        authenticationService.signOut()
                                    }
                                } )
                            ).subscribe();
                        }

                        // Wait on refreshed access token
                        return this.refreshedAccessToken.pipe(
                            filter(( token ) => token != null ),
                            take( 1 ),
                            switchMap(( accessToken: string ) => {

                                // Retry with the refresh access token
                                authRequest = jwtRequest( req, accessToken )
                                return next.handle( authRequest )
                            } )
                        )
                    }
                }

                // Propagate the failure
                return throwError( error )
            } ),

            // Log the response
            map(( event: HttpEvent<any> ) => {
                if ( event instanceof HttpResponse ) {
                    logInteraction( console.log, authRequest, 'HttpResponse', event )
                }
                return event;
            } )
        )
    }

}