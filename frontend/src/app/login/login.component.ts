import { Component, OnInit } from '@angular/core';
import { SocialUser } from "angularx-social-login";
import { AuthenticationService } from '../authentication.service';
import { GoogleLoginProvider } from "angularx-social-login";

@Component( {
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css']
} )
export class LoginComponent implements OnInit {

    user: SocialUser;

    constructor( private authenticationService: AuthenticationService ) {
        this.authenticationService.authenticationState().subscribe(( user: SocialUser ) => this.user = user );
    }

    ngOnInit() {
    }

    signIn(): void {
        this.authenticationService.signIn();
    }

    signOut(): void {
        this.authenticationService.signOut();
    }

}