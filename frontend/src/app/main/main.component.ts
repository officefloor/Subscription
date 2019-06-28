import { Component, OnInit } from '@angular/core'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { ServerApiService, parseDate, isExpired, isExpireSoon, Domain } from '../server-api.service'
import * as moment from 'moment'
import { Sort } from '@angular/material/sort'
import { Router } from '@angular/router'
import { AlertService } from '../alert.service'
import { of } from 'rxjs'
import { concatFMap } from '../rxjs.util'
import { Array } from 'core-js'

@Component( {
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css']
} )
export class MainComponent implements OnInit {

    domains: Array<DomainRow> = []

    sortedDomains: Array<DomainRow> = []

    constructor(
        private authentication: AuthenticationService,
        private serverApiService: ServerApiService,
        private alertService: AlertService,
    ) {}

    ngOnInit() {
        this.authentication.authenticationState().pipe(
            concatFMap(( user: SocialUser ) => user ? this.serverApiService.getDomains() : of( [] ) ),
        ).subscribe(( domains: Domain[] ) => {

            // Load the new domains
            this.domains = []
            for ( let domain of domains ) {
                const expireMoment = parseDate( domain.expiresDate )

                // Create the domain row
                const domainRow: DomainRow = {
                    ...domain,
                    localExpires: expireMoment.format( "D MMM YYYY" ),
                    sortExpires: expireMoment.unix(),
                    timeAgo: expireMoment.fromNow(),
                    isExpired: isExpired( expireMoment ),
                    isExpireSoon: isExpireSoon( expireMoment ),
                }
                this.domains.push( domainRow )
            }

            // Track new domains
            this.sortedDomains = this.domains.slice()

        }, (error) => {
            // Error, so no domains
            this.domains = []
            this.sortedDomains = []
            
            // Notify of the error
            this.alertService.error(error)
        } )
    }

    sortDomains( sort: Sort ) {
        const data: DomainRow[] = this.domains.slice()
        if ( !sort.active || sort.direction === '' ) {
            this.sortedDomains = data
            return
        }

        this.sortedDomains = data.sort(( a: DomainRow, b: DomainRow ) => {
            const isAsc = sort.direction === 'asc'
            switch ( sort.active ) {
                case 'domain': return this.compare( a.domainName, b.domainName, isAsc )
                case 'expireDate': return this.compare( a.sortExpires, b.sortExpires, isAsc )
                case 'expire': return this.compare( a.sortExpires, b.sortExpires, isAsc )
                default: return 0;
            }
        } )
    }

    compare( a: number | string, b: number | string, isAsc: boolean ) {
        return ( a < b ? -1 : 1 ) * ( isAsc ? 1 : -1 )
    }
}

class DomainRow implements Domain {
    domainName: string
    expiresDate: string
    localExpires: string
    sortExpires: number
    timeAgo: string
    isExpired: boolean
    isExpireSoon: boolean
}