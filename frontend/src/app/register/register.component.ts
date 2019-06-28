import { Component, OnInit } from '@angular/core'
import { FormGroup, FormControl, ValidationErrors, FormBuilder } from '@angular/forms'
import { Router } from '@angular/router'

@Component( {
    selector: 'app-register',
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.css']
} )
export class RegisterComponent implements OnInit {

    isValidDomainName: boolean = false

    registerDomainNameError: string = null

    registerDomainForm: FormGroup

    constructor(
        private router: Router,
        private formBuilder: FormBuilder,
    ) { }

    ngOnInit() {
        this.registerDomainForm = this.formBuilder.group( {
            domainName: this.formBuilder.control( '', ( formControl ) => {
                this.registerDomainNameError = this.checkDomainName( formControl.value )
                return {}
            } )
        } )
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