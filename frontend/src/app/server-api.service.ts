import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

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
}

export interface AuthenticateResponse {
    refreshToken: string
    accessToken: string
}

export interface AccessTokenResponse {
    accessToken: string
}

export interface Configuration {
    paypalEnvironment: string
    paypalClientId: string
    paypalClientSecret: string
}

export interface Domain {
    domainName: string
    expires: number
}

export interface DomainPayment {
    paymentDate: number
    extendsSubscriptionToDate: number
    isSubscriptionReset: boolean
    user: User
}

export interface User {
    id: number
    name: string
    email: string
    photoUrl: string
}

export interface Invoice {

}