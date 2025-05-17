/**
 * Animation and effects for auction success
 */
document.addEventListener('DOMContentLoaded', function() {
    // Check if there's a success message
    const successMessage = document.querySelector('.alert-success');
    
    if (successMessage) {
        // Highlight elements with a brief animation
        const highlightElements = document.querySelectorAll('.card-title, .current-price');
        highlightElements.forEach(function(element) {
            element.classList.add('highlight-animation');
            
            // Remove animation class after it completes
            setTimeout(function() {
                element.classList.remove('highlight-animation');
            }, 2000);
        });
        
        // Auto-dismiss the alert after 5 seconds
        setTimeout(function() {
            const closeButton = successMessage.querySelector('.btn-close');
            if (closeButton) {
                closeButton.click();
            } else {
                successMessage.style.display = 'none';
            }
        }, 5000);
    }
    
    // Add highlight animation CSS if not already in the document
    if (!document.getElementById('highlight-animation-style')) {
        const style = document.createElement('style');
        style.id = 'highlight-animation-style';
        style.textContent = `
            @keyframes highlight {
                0% { background-color: transparent; }
                30% { background-color: rgba(40, 167, 69, 0.2); }
                100% { background-color: transparent; }
            }
            .highlight-animation {
                animation: highlight 2s ease-out;
            }
        `;
        document.head.appendChild(style);
    }
    
    // For newly created auctions, pulse the auction details section
    const isNewlyCreated = new URLSearchParams(window.location.search).get('new') === 'true';
    if (isNewlyCreated) {
        const auctionDetailsCard = document.querySelector('.card');
        if (auctionDetailsCard) {
            auctionDetailsCard.classList.add('border-success');
            setTimeout(() => {
                auctionDetailsCard.classList.remove('border-success');
            }, 3000);
        }
    }
}); 