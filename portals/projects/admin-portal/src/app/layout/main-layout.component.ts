import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterLink, RouterLinkActive, RouterOutlet,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule,
  ],
  template: `
    <mat-sidenav-container style="height: 100vh">
      <mat-sidenav mode="side" opened style="width: 240px">
        <mat-toolbar color="primary">FX Admin</mat-toolbar>
        <mat-nav-list>
          <a mat-list-item routerLink="/currencies" routerLinkActive="active">
            <mat-icon matListItemIcon>attach_money</mat-icon>
            <span matListItemTitle>Currencies</span>
          </a>
          <a mat-list-item routerLink="/currency-pairs" routerLinkActive="active">
            <mat-icon matListItemIcon>swap_horiz</mat-icon>
            <span matListItemTitle>Currency Pairs</span>
          </a>
          <a mat-list-item routerLink="/trade-books" routerLinkActive="active">
            <mat-icon matListItemIcon>menu_book</mat-icon>
            <span matListItemTitle>Trade Books</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary">
          <span>FX Master Data Admin</span>
          <span class="spacer"></span>
          <a mat-button href="http://localhost:8083/swagger-ui.html" target="_blank">
            <mat-icon>api</mat-icon> API
          </a>
        </mat-toolbar>
        <div style="padding: 24px">
          <router-outlet></router-outlet>
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .active { background: rgba(0,0,0,0.06); }
  `]
})
export class MainLayoutComponent {}
