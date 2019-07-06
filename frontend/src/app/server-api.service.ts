import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Observable } from 'rxjs'
import * as moment from 'moment'

declare let window: any

const DATE_FORMAT = 'ddd, D MMM YYYY H:mm:ss [GMT]'

export function formatDate( date: moment.Moment ) {
    return date.format( DATE_FORMAT )
}

export function parseDate( value: string ): moment.Moment {
    return moment( value, DATE_FORMAT )
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
    isAuthenticationRequired: boolean
    googleClientId: string
    paypalClientId: string
    paypalCurrency: string
}

export interface Administrator {
    googleId: string
    notes: string
}

export interface Configuration {
    googleClientId: string
    administrators: Array<Administrator>
    paypalEnvironment: string
    paypalClientId: string
    paypalClientSecret: string
    paypalInvoiceIdTemplate: string
    paypalCurrency: string
}

export interface Domain {
    domainName: string
    expiresDate: string
}

export interface DomainPayments {
    domainName: string
    expiresDate: string
    payments: Array<Subscription>
}

export interface Subscription {
    paymentDate: string
    extendsToDate: string
    isRestartSubscription: boolean
    paidByName: string
    paidByEmail: string
    paymentOrderId: string
    paymentReceipt: string
    paymentAmount: number
}

export interface CreatedInvoice {
    orderId: string
    status: string
    invoiceId: string
}


@Injectable( {
    providedIn: 'root'
} )
export class ServerApiService {

    // Direct to separate server port when running in development
    private serverUrl: string = window.location.href.startsWith( 'http://localhost:4200' ) ? 'http://localhost:8080' : ''

    constructor(
        private http: HttpClient
    ) { }

    public isServerUrl( url: string ): boolean {
        return true
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

    public updateConfiguration( configuration: Configuration ): Observable<void> {
        return this.http.post<void>( `${this.serverUrl}/configuration`, configuration )
    }

    public getDomains(): Observable<Domain[]> {
        return this.http.get<Domain[]>( `${this.serverUrl}/domains` )
    }

    public getDomainSubscriptions( domainName: string ): Observable<DomainPayments> {
        return this.http.get<DomainPayments>( `${this.serverUrl}/subscriptions/domain/${domainName}` )
    }

    public createInvoice( domainName: string, isRestartSubscription: boolean ): Observable<CreatedInvoice> {
        return this.http.post<CreatedInvoice>( `${this.serverUrl}/invoices/domain/${domainName}?restart=${isRestartSubscription}`, null )
    }

    public capturePayment( orderId: string ): Observable<DomainPayments> {
        return this.http.post<DomainPayments>( `${this.serverUrl}/payments/domain/${orderId}`, null )
    }

}