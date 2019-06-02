import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import * as moment from 'moment'

declare let window: any

@Injectable( {
    providedIn: 'root'
} )
export class ServerApiService {

    // Direct to separate server port when running in development
    private serverUrl: string = environment.production ? '' : window.location.href.startsWith( 'http://localhost:4200' ) ? 'http://localhost:8080' : '';

    constructor( private http: HttpClient ) {
    }

    public authenticate( idToken: string ): Observable<AuthenticateResponse> {
        return this.http.post<AuthenticateResponse>( `${this.serverUrl}/authenticate`, {
            idToken: idToken
        } )
    }

    public refreshAccessToken( refreshToken: string ): Observable<AccessTokenResponse> {
        return this.http.post<AccessTokenResponse>( `${this.serverUrl}/refreshAccessToken`, {
            refreshToken: refreshToken
        } )
    }

    public getInitialisation(): Observable<Initialisation> {
        return this.http.get<Initialisation>( `${this.serverUrl}/initialise` )
    }

    public getConfiguration(): Observable<Configuration> {
        return this.http.get<Configuration>( `${this.serverUrl}/configuration` )
    }

    public getDefaultConfiguration(): Observable<Configuration> {
        return this.http.get<Configuration>( `${this.serverUrl}/configuration/default` )
    }

    public updateConfiguration( configuration: Configuration ): Observable<void> {
        return this.http.post<void>( `${this.serverUrl}/configuration`, configuration )
    }

    public getDomains(): Observable<Domain[]> {
        return this.http.get<Domain[]>( `${this.serverUrl}/domains` )
    }

    public getDomainPayments( domainName: string ): Observable<DomainPayments> {
        return this.http.get<DomainPayments>( `${this.serverUrl}/payments/domain/${domainName}` )
    }
}

export function parseDate( value: string ): moment.Moment {
    return moment( value, 'ddd, D MMM YYYY H:mm:ss [GMT]' )
}

export function isExpired( date: moment.Moment ): boolean {
    return moment().isAfter( date )
}

export function isExpireSoon( date: moment.Moment ): boolean {
    return moment().add( 1, 'month' ).isAfter( date )
}

export interface AuthenticateResponse {
    refreshToken: string
    accessToken: string
}

export interface AccessTokenResponse {
    accessToken: string
}

export interface Initialisation {
    googleClientId: string
    paypalClientId: string
}

export interface Configuration {
    paypalEnvironment: string
    paypalClientId: string
    paypalClientSecret: string
}

export interface Domain {
    domainName: string
    expiresDate: string
}

export interface DomainPayments {
    domainName: string
    expiresDate: string
    payments: Payment[]
}

export interface Payment {
    paymentDate: string
    extendsToDate: string
    isRestartSubscription: boolean
    paidByName: string
    paidByEmail: string
    paymentOrderId: string
}
