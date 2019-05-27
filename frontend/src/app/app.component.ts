import { Component } from '@angular/core';
import { SocialUser } from "angularx-social-login";
import { AuthenticationService } from './authentication.service';

@Component( {
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
} )
export class AppComponent {

    user: SocialUser;

    constructor( private authenticationService: AuthenticationService ) {
        this.authenticationService.authenticationState().subscribe(( user: SocialUser ) => this.user = user );
    }


    title = 'OfficeFloor Subscription';
}