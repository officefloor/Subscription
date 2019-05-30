import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from '../authentication.service';
import { SocialUser } from "angularx-social-login";

@Component( {
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css']
} )
export class MainComponent implements OnInit {

    user: SocialUser = null;

    constructor( private authentication: AuthenticationService ) {
    }

    ngOnInit() {
        this.authentication.authenticationState().subscribe(( user: SocialUser ) => {

            // Flag the user to indicate logged in
            this.user = user

            // Nothing further if not logged in
            if ( !this.user ) {
                return
            }

            // Load the domains
            
        } )
    }

}
