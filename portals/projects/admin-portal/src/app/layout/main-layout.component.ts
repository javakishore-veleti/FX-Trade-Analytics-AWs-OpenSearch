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
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="shell__sidenav">
        <div class="brand">
          <div class="brand__mark">
            <mat-icon>show_chart</mat-icon>
          </div>
          <div class="brand__text">
            <div class="brand__title">FX Analytics</div>
            <div class="brand__sub">Admin Console</div>
          </div>
        </div>

        <div class="nav-section">
          <div class="nav-section__label">Master Data</div>
          <a class="nav-item" routerLink="/currencies" routerLinkActive="nav-item--active">
            <mat-icon>payments</mat-icon>
            <span>Currencies</span>
          </a>
          <a class="nav-item" routerLink="/currency-pairs" routerLinkActive="nav-item--active">
            <mat-icon>compare_arrows</mat-icon>
            <span>Currency Pairs</span>
          </a>
          <a class="nav-item" routerLink="/trade-books" routerLinkActive="nav-item--active">
            <mat-icon>menu_book</mat-icon>
            <span>Trade Books</span>
          </a>
        </div>

        <div class="nav-section">
          <div class="nav-section__label">Administration</div>
          <a class="nav-item" routerLink="/opensearch-deployments" routerLinkActive="nav-item--active">
            <mat-icon>cloud</mat-icon>
            <span>OpenSearch (AWS)</span>
          </a>
          <a class="nav-item" routerLink="/trades-search" routerLinkActive="nav-item--active">
            <mat-icon>search</mat-icon>
            <span>Trades Search</span>
          </a>
        </div>

        <div class="sidebar-foot">
          <a mat-stroked-button color="primary" href="http://localhost:9083/swagger-ui.html" target="_blank" class="full-width">
            <mat-icon>api</mat-icon> API Reference
          </a>
        </div>
      </mat-sidenav>

      <mat-sidenav-content class="shell__content">
        <header class="topbar">
          <div class="topbar__crumbs">
            <span class="topbar__home">Admin</span>
            <mat-icon class="topbar__sep">chevron_right</mat-icon>
            <span class="topbar__page">Master Data</span>
          </div>
          <div class="spacer"></div>
          <div class="topbar__env">
            <span class="env-dot"></span> dev — H2
          </div>
          <a mat-icon-button href="http://localhost:8085" target="_blank" matTooltip="Kafka UI">
            <mat-icon>hub</mat-icon>
          </a>
          <a mat-icon-button href="http://localhost:5601" target="_blank" matTooltip="OpenSearch Dashboards">
            <mat-icon>dashboard</mat-icon>
          </a>
        </header>

        <main class="page">
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; background: var(--surface-page); }

    .shell__sidenav {
      width: 264px;
      background: var(--surface-card);
      border-right: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      padding: 20px 14px 14px 14px;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 8px 20px 8px;
      margin-bottom: 8px;
      border-bottom: 1px solid var(--border-subtle);
    }
    .brand__mark {
      width: 40px; height: 40px;
      border-radius: 12px;
      background: var(--brand-grad);
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 6px 16px rgba(15, 42, 92, 0.25);
    }
    .brand__title { font-weight: 700; font-size: 15px; color: var(--text-strong); letter-spacing: -0.01em; }
    .brand__sub   { font-size: 12px; color: var(--text-muted); }

    .nav-section { margin-top: 16px; }
    .nav-section__label {
      font-size: 11px; font-weight: 600;
      color: var(--text-subtle);
      text-transform: uppercase;
      letter-spacing: 0.08em;
      padding: 0 12px 8px 12px;
    }

    .nav-item {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 12px;
      border-radius: 10px;
      color: var(--text-default);
      text-decoration: none;
      font-weight: 500;
      font-size: 14px;
      cursor: pointer;
      transition: background 120ms ease, color 120ms ease;
    }
    .nav-item:hover { background: var(--surface-soft); }
    .nav-item mat-icon { color: var(--text-muted); transition: color 120ms ease; }

    .nav-item--active {
      background: var(--mat-sys-primary-container);
      color: var(--brand-navy);
      font-weight: 600;
    }
    .nav-item--active mat-icon { color: var(--brand-navy); }

    .sidebar-foot { margin-top: auto; padding-top: 12px; }

    .topbar {
      height: 64px;
      display: flex; align-items: center; gap: 12px;
      padding: 0 24px;
      background: var(--surface-card);
      border-bottom: 1px solid var(--border-subtle);
      box-shadow: 0 1px 0 rgba(15, 42, 92, 0.02);
    }
    .topbar__crumbs { display: flex; align-items: center; gap: 6px; font-size: 14px; color: var(--text-muted); }
    .topbar__home { font-weight: 500; }
    .topbar__sep { font-size: 18px; width: 18px; height: 18px; color: var(--text-subtle); }
    .topbar__page { font-weight: 600; color: var(--text-strong); }
    .topbar__env {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 6px 12px;
      background: #ECFDF5; color: #065F46;
      border-radius: 999px;
      font-size: 12px; font-weight: 600;
    }
    .env-dot { width: 8px; height: 8px; border-radius: 50%; background: #10B981; box-shadow: 0 0 0 3px rgba(16,185,129,0.18); }

    .page { padding: 28px 32px; max-width: 1400px; }
  `]
})
export class MainLayoutComponent {}
