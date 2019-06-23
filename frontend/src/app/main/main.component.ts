import { Component, OnInit } from '@angular/core'
import { AuthenticationService } from '../authentication.service'
import { SocialUser } from "angularx-social-login"
import { ServerApiService, parseDate, isExpired, isExpireSoon, Domain } from '../server-api.service'
import * as moment from 'moment'
import { Sort } from '@angular/material/sort'
import { FormGroup, FormControl, ValidationErrors } from '@angular/forms'
import { Router } from '@angular/router'
import { AlertService } from '../alert.service'
import { iif, of } from 'rxjs'
import { mergeMap, tap } from 'rxjs/operators'

@Component( {
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css']
} )
export class MainComponent implements OnInit {

    domains: DomainRow[] = []

    sortedDomains: DomainRow[] = []

    isValidDomainName: boolean = false

    registerDomainNameError: string = null

    registerDomainForm = new FormGroup( {
        domainName: new FormControl( '', ( formControl ) => {
            this.registerDomainNameError = this.checkDomainName( formControl.value )
            return {}
        } )
    } )

    constructor(
        private authentication: AuthenticationService,
        private serverApiService: ServerApiService,
        private router: Router,
        private alertService: AlertService,
    ) { }

    ngOnInit() {
        this.authentication.authenticationState().pipe(
            mergeMap(( user: SocialUser | null ) => iif(() => user !== null,
                this.serverApiService.getDomains().pipe(
                    tap(( domains: Domain[] ) => {

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
                    } )
                )
            ) ),
            this.alertService.alertError(( error: any ) => {

                // Error, so no domains
                this.domains = []
                this.sortedDomains = []

                // Provide alert regarding error
                return true
            } )
        ).subscribe()
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

    checkDomainName( name: string ): string {
        this.isValidDomainName = false
        name = name.trim()
        if ( name.length == 0 ) {
            return null // only invalid on submitting
        } else if ( name.match( /\s/ ) ) {
            return 'May not contain spaces'
        } else if ( name.startsWith( '.' ) ) {
            return "May not start with '.'"
        } else if ( name.endsWith( '.' ) ) {
            return "May not end with '.'"
        } else if ( !name.includes( '.' ) ) {
            return "Must contain at least one '.' (e.g. domain.com)"
        }
        this.isValidDomainName = true
        return null // valid domain name            
    }

    registerDomain() {
        const domainName: string = this.registerDomainForm.value.domainName

        // Ensure valid domain name
        if ( domainName.trim().length === 0 ) {
            this.registerDomainNameError = "Must provide domain name"
            return
        }
        this.registerDomainNameError = this.checkDomainName( domainName )
        if ( this.registerDomainNameError ) {
            return
        }

        // Route to domain
        this.router.navigate( ["domain", domainName] )
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