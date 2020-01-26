import { Component, OnInit } from '@angular/core'
import { SocialUser } from 'angularx-social-login'
import { AuthenticationService } from './authentication.service'
import { Router, RoutesRecognized } from '@angular/router'

@Component( {
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
} )
export class AppComponent implements OnInit {

    isInitialised: boolean = false

    user: SocialUser = null

    isSecure: boolean = true

    constructor(
        private authenticationService: AuthenticationService,
		private router: Router
    ) { }

    ngOnInit(): void {

        // Determine when ready
        this.authenticationService.readyState().subscribe(( isReady ) => this.isInitialised = isReady )

        // Keep track of logged in user
        this.authenticationService.authenticationState().subscribe(( user ) => this.user = user )

        // Initialise authentication
        this.authenticationService.initialise().subscribe()

		// Listen to routing to provide indicating if secure page
		this.router.events.subscribe(event => {
			if (event instanceof RoutesRecognized) {
				const route = event.state.root.firstChild
				this.isSecure = !route.data['insecure']
			}
		})
    }

}