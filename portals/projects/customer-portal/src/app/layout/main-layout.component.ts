import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatToolbarModule, MatTabsModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>trending_up</mat-icon>
      <span style="margin-left: 8px">FX Customer Portal</span>
    </mat-toolbar>

    <nav mat-tab-nav-bar [tabPanel]="tabPanel">
      <a mat-tab-link routerLink="/place" routerLinkActive #place="routerLinkActive" [active]="place.isActive">Place Trade</a>
      <a mat-tab-link routerLink="/recent" routerLinkActive #recent="routerLinkActive" [active]="recent.isActive">Recent Trades</a>
    </nav>
    <mat-tab-nav-panel #tabPanel></mat-tab-nav-panel>

    <div style="padding: 24px; max-width: 1100px; margin: 0 auto">
      <router-outlet></router-outlet>
    </div>
  `,
})
export class MainLayoutComponent {}
