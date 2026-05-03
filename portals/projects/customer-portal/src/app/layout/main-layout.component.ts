import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatIconModule, MatButtonModule],
  template: `
    <header class="topbar">
      <div class="topbar__inner">
        <a routerLink="/" class="brand">
          <div class="brand__mark">
            <mat-icon>candlestick_chart</mat-icon>
          </div>
          <div class="brand__text">
            <div class="brand__title">FX Trade</div>
            <div class="brand__sub">Customer Portal</div>
          </div>
        </a>

        <nav class="nav">
          <a class="nav__link" routerLink="/place" routerLinkActive="nav__link--active">
            <mat-icon>swap_horiz</mat-icon>
            <span>Place Trade</span>
          </a>
          <a class="nav__link" routerLink="/recent" routerLinkActive="nav__link--active">
            <mat-icon>receipt_long</mat-icon>
            <span>Recent Trades</span>
          </a>
        </nav>

        <div class="topbar__right">
          <span class="topbar__pill">
            <span class="dot"></span> Live
          </span>
        </div>
      </div>
    </header>

    <main class="page">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .topbar {
      background: var(--surface-card);
      border-bottom: 1px solid var(--border-subtle);
      box-shadow: 0 1px 0 rgba(15, 118, 110, 0.04);
      position: sticky;
      top: 0;
      z-index: 10;
    }
    .topbar__inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 28px;
      height: 72px;
      display: flex;
      align-items: center;
      gap: 32px;
    }

    .brand { display: flex; align-items: center; gap: 12px; text-decoration: none; color: inherit; }
    .brand__mark {
      width: 42px; height: 42px;
      border-radius: 12px;
      background: var(--brand-grad);
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 18px rgba(13, 148, 136, 0.30);
    }
    .brand__title { font-weight: 700; font-size: 16px; color: var(--text-strong); letter-spacing: -0.01em; }
    .brand__sub   { font-size: 12px; color: var(--text-muted); }

    .nav { display: flex; gap: 4px; margin-left: 24px; }
    .nav__link {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 10px 16px;
      border-radius: 10px;
      color: var(--text-muted);
      text-decoration: none;
      font-weight: 500; font-size: 14px;
      transition: background 120ms ease, color 120ms ease;
    }
    .nav__link:hover { background: var(--surface-soft); color: var(--text-default); }
    .nav__link mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .nav__link--active {
      background: var(--mat-sys-primary-container);
      color: var(--brand-teal);
      font-weight: 600;
    }

    .topbar__right { margin-left: auto; display: flex; align-items: center; gap: 12px; }
    .topbar__pill {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 6px 12px;
      background: #ECFDF5; color: #065F46;
      border-radius: 999px;
      font-size: 12px; font-weight: 600;
    }
    .dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      background: #10B981;
      box-shadow: 0 0 0 3px rgba(16,185,129,0.18);
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse {
      0%, 100% { box-shadow: 0 0 0 3px rgba(16,185,129,0.18); }
      50%      { box-shadow: 0 0 0 6px rgba(16,185,129,0.10); }
    }

    .page { max-width: 1200px; margin: 0 auto; padding: 32px 28px 64px 28px; }
  `]
})
export class MainLayoutComponent {}
