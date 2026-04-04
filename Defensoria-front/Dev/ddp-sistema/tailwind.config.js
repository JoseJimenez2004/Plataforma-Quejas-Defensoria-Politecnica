/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./projects/ddp-shell/src/**/*.{html,ts}",
    "./projects/mfe-quejoso/src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'ipn-maroon': '#611232',
      },
    },
  },
  plugins: [],
}