<script lang="ts">
	import { page } from '$app/stores';

	const items = [
		{ href: '/', label: 'Workflows', icon: '☰' },
		{ href: '/health', label: 'Santé', icon: '♥' },
		{ href: '/settings', label: 'Réglages', icon: '⚙' }
	];
</script>

<nav>
	{#each items as item}
		<a href={item.href} class:active={$page.url.pathname === item.href}>
			<span class="icon">{item.icon}</span>
			<span>{item.label}</span>
		</a>
	{/each}
</nav>

<style>
	nav {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		display: flex;
		background: var(--bg-elevated);
		border-top: 1px solid var(--border);
		padding-bottom: env(safe-area-inset-bottom);
	}
	a {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
		padding: 10px 0 8px;
		color: var(--text-muted);
		font-size: 0.75rem;
	}
	a.active {
		color: var(--accent);
	}
	.icon {
		font-size: 1.1rem;
	}
</style>
