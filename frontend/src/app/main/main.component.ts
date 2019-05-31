import { Component, OnInit } from '@angular/core'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { ServerApiService, Domain } from '../server-api.service'
import * as moment from 'moment'
import { Sort } from '@angular/material/sort'

@Component( {
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css']
} )
export class MainComponent implements OnInit {

    domains: DomainRow[] = []

    sortedDomains: DomainRow[] = []

    constructor(
        private authentication: AuthenticationService,
        private serverApiService: ServerApiService
    ) {
    }

    ngOnInit() {
        this.authentication.authenticationState().subscribe(( user: SocialUser ) => {

            // Nothing further if not logged in
            if ( !user ) {
                return
            }

            // Load the domains
            this.serverApiService.getDomains().subscribe(( domains: Domain[] ) => {

                // Obtain current time
                const now: moment.Moment = moment()
                const expireSoon: moment.Moment = moment().add( 1, 'month' )

                // Load the new domains
                this.domains = []
                for ( let domain of domains ) {
                    const expireMoment = moment( domain.expiresDate )
                    const localExpires = expireMoment.format( "D MMM YYYY" )
                    const sortExpires = expireMoment.unix()
                    const timeAgo = expireMoment.fromNow()
                    const isExpired = now.isAfter( expireMoment )
                    const isExpireSoon = expireSoon.isAfter( expireMoment )

                    // Create the domain row
                    const domainRow: DomainRow = {
                        ...domain,
                        localExpires: localExpires,
                        sortExpires: sortExpires,
                        timeAgo: timeAgo,
                        isExpired: isExpired,
                        isExpireSoon: isExpireSoon,
                    }
                    this.domains.push( domainRow )
                }

                // Track new domains
                this.sortedDomains = this.domains.slice()

            }, ( error: any ) => {
                console.log( 'TODO handle error', error )
            } )
        } )
    }

    sortDomains( sort: Sort ) {
        const data: DomainRow[] = this.domains.slice()
        if ( !sort.active || sort.direction === '' ) {
            this.sortedDomains = this.domains
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
        return ( a < b ? -1 : 1 ) * ( isAsc ? 1 : -1 );
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