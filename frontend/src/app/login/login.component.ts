import { Component, OnInit } from '@angular/core';
import { SocialUser } from "angularx-social-login";
import { AuthenticationService } from '../authentication.service';
import { GoogleLoginProvider } from "angularx-social-login";
import { InitialiseService } from '../initialise.service'
import { Initialisation } from '../server-api.service'

@Component( {
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css']
} )
export class LoginComponent implements OnInit {

    isAuthenticationRequired: boolean = true

    user: SocialUser

    constructor(
        private initialiseService: InitialiseService,
        private authenticationService: AuthenticationService
    ) { }

    ngOnInit() {
        // Determine if show login
        this.initialiseService.initialisation().subscribe(( initialisation: Initialisation ) => this.isAuthenticationRequired = initialisation.isAuthenticationRequired )

        // Monitor user
        this.authenticationService.authenticationState().subscribe(( user: SocialUser ) => this.user = user );
    }

    signIn(): void {
        this.authenticationService.signIn();
    }

    signOut(): void {
        this.authenticationService.signOut();
    }

}