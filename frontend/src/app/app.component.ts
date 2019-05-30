import { Component } from '@angular/core';
import { SocialUser } from "angularx-social-login";
import { AuthenticationService } from './authentication.service';

@Component( {
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
} )
export class AppComponent {

    isInitialised: boolean = false

    user: SocialUser

    constructor( private authenticationService: AuthenticationService ) {

        // Determine when ready
        this.authenticationService.readyState().subscribe(( isReady: boolean ) => this.isInitialised = isReady )


        // Keep track of logged in user
        this.authenticationService.authenticationState().subscribe(( user: SocialUser ) => this.user = user )
    }

}