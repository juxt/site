module.exports = {
    content: ['../request-template.html'],
    theme: {
        extend: {
            colors: {
                orange: require('tailwindcss/colors').orange
            }
        },
    },
    variants: {},
    plugins: [require('@tailwindcss/forms'),
              require('@tailwindcss/typography'),
              require('@tailwindcss/aspect-ratio'),
              require('@tailwindcss/line-clamp'),
             ],
}
