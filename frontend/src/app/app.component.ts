import { Component, OnInit } from '@angular/core'
import { SocialUser } from 'angularx-social-login'
import { AuthenticationService } from './authentication.service'

@Component( {
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
} )
export class AppComponent implements OnInit {

    isInitialised: boolean = false

    user: SocialUser = null

    constructor(
        private authenticationService: AuthenticationService
    ) { }

    ngOnInit(): void {

        // Determine when ready
        this.authenticationService.readyState().subscribe(( isReady ) => this.isInitialised = isReady )

        // Keep track of logged in user
        this.authenticationService.authenticationState().subscribe(( user ) => this.user = user )

        // Initialise authentication
        this.authenticationService.initialise()
    }

}