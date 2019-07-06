import { Injectable } from '@angular/core'
import { AuthService, SocialUser } from "angularx-social-login"
import { GoogleLoginProvider } from "angularx-social-login"
import { ServerApiService, AuthenticateResponse, AccessTokenResponse, Initialisation, parseDate } from './server-api.service'
import { Observable, BehaviorSubject, of, throwError } from 'rxjs'
import { map, catchError, filter } from 'rxjs/operators'
import { concatFMap } from './rxjs.util'
import { InitialiseService } from './initialise.service'
import { AlertService } from './alert.service'
import * as moment from 'moment'

@Injectable( {
    providedIn: 'root'
} )
export class AuthenticationService {

    public static readonly REFRESH_TOKEN = "refreshToken"

    public static readonly REFRESH_EXPIRE = "refreshExpire"

    public static readonly ACCESS_TOKEN = "accessToken"

    public static readonly ACCESS_EXPIRE = "accessExpire"

    // Login state
    private state: BehaviorSubject<SocialUser> = new BehaviorSubject<SocialUser>( null )

    // Ready state
    private ready: BehaviorSubject<boolean> = new BehaviorSubject<boolean>( false )

    constructor(
        private initialiseService: InitialiseService,
        private authService: AuthService,
        private serverApiService: ServerApiService,
        private alertService: AlertService,
    ) { }

    initialise(): Observable<Initialisation> {
        return this.initialiseService.initialisation().pipe(
            concatFMap(( initialisation: Initialisation ) => {

                // Determine if requires authentication
                if ( !initialisation.isAuthenticationRequired ) {
                    this.authService = null
                    this.ready.next( true )
                    const user = new SocialUser()
                    user.name = 'No Authentication'
                    this.state.next( user )
                    return of( initialisation )
                }

                // Determine when ready
                return this.authService.readyState.pipe(
                    filter(( ready: any ) => {
                        const isReady: boolean = ready[0] && ( ready[0] === 'GOOGLE' )
                        if ( isReady ) {
                            this.ready.next( isReady ) // flag ready
                        }
                        return isReady
                    } ),
                    concatFMap(( isReady: boolean ) => this.authService.authState ),
                    concatFMap(( user: SocialUser ) => {

                        // Ensure logged in
                        if ( !user ) {
                            return of( initialisation )
                        }

                        // Determine if cached refresh token
                        const refreshToken = localStorage.getItem( AuthenticationService.REFRESH_TOKEN )
                        if ( refreshToken ) {

                            // Determine if expired
                            const refreshExpire = localStorage.getItem( AuthenticationService.REFRESH_EXPIRE )
                            const expire = parseDate( refreshExpire )
                            if ( expire.isValid() && ( moment().isBefore( expire ) ) ) {

                                // Flag authenticated
                                this.state.next( user )
                                return of( initialisation )
                            }
                        }

                        // User logged in
                        return this.serverApiService.authenticate( user.idToken ).pipe(
                            concatFMap(( response: AuthenticateResponse ) => {

                                // Capture the tokens
                                const refreshToken: string = response.refreshToken
                                const accessToken: string = response.accessToken

                                // Store the tokens
                                localStorage.setItem( AuthenticationService.REFRESH_TOKEN, refreshToken )
                                localStorage.setItem( AuthenticationService.ACCESS_TOKEN, accessToken )

                                // Notify logged in
                                this.state.next( user )

                                // Initialised
                                return of( initialisation )
                            } )
                        )
                    } )
                )
            } )
        )
    }

    private handleError( error: any ) {

        // Error with login, so not logged in
        this.state.next( null )

        // Notify of error
        this.alertService.error( error )
    }

    public signIn(): void {
        this.authService.signIn( GoogleLoginProvider.PROVIDER_ID )
    }

    public signOut(): void {

        // Clear the tokens
        localStorage.removeItem( AuthenticationService.REFRESH_TOKEN )
        localStorage.removeItem( AuthenticationService.ACCESS_TOKEN )

        // Google sign-out
        this.authService.signOut()
    }

    public refreshAccessToken(): Observable<AccessTokenResponse> {

        // Obtain the refresh token
        const refreshToken: string = localStorage.getItem( AuthenticationService.REFRESH_TOKEN )
        if ( !refreshToken ) {
            return of( null ) // no refresh token so no access token
        }

        // Undertake refreshing the access token
        return this.serverApiService.refreshAccessToken( refreshToken ).pipe(
            map(( response: AccessTokenResponse ) => {

                // Capture the new access token
                localStorage.setItem( AuthenticationService.ACCESS_TOKEN, response.accessToken )

                // Return the response
                return response
            } ) )
    }

    public getAccessToken(): string {
        return localStorage.getItem( AuthenticationService.ACCESS_TOKEN )
    }

    public authenticationState(): Observable<SocialUser> {
        return this.state
    }

    public readyState(): Observable<boolean> {
        return this.ready;
    }

}