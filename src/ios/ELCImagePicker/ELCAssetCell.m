//
//  AssetCell.m
//
//  Created by ELC on 2/15/11.
//  Copyright 2011 ELC Technologies. All rights reserved.
//

#import "ELCAssetCell.h"
#import "ELCAsset.h"

@interface ELCAssetCellSelectedView : UIView
@property (nonatomic, strong) UILabel * label;
@property (nonatomic, strong) UIView * border;
@end
@implementation ELCAssetCellSelectedView
@end

@interface ELCAssetCell ()

@property (nonatomic, strong) NSArray *rowAssets;
@property (nonatomic, strong) NSMutableArray *imageViewArray;
@property (nonatomic, strong) NSMutableArray *overlayViewArray;

@end

@implementation ELCAssetCell

const int LABEL_SIZE = 20;

//Using auto synthesizers

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseIdentifier];
	if (self) {
        UITapGestureRecognizer *tapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(cellTapped:)];
        [self addGestureRecognizer:tapRecognizer];
        
        NSMutableArray *mutableArray = [[NSMutableArray alloc] initWithCapacity:4];
        self.imageViewArray = mutableArray;
        
        NSMutableArray *overlayArray = [[NSMutableArray alloc] initWithCapacity:4];
        self.overlayViewArray = overlayArray;
	}
	return self;
}

-(void)setTextOnOverlay:(ELCAsset*)asset overlayView:(ELCAssetCellSelectedView*)overlayView
{
    overlayView.label.text = [NSString stringWithFormat:@"%d", (int)[self.delegate getSelectedIndexForAsset:asset sender:self]];
}

- (void)setAssets:(NSArray *)assets
{
    self.rowAssets = assets;
	for (UIImageView *view in _imageViewArray) {
        [view removeFromSuperview];
	}
    for (UIImageView *view in _overlayViewArray) {
        [view removeFromSuperview];
	}
    //set up a pointer here so we don't keep calling [UIImage imageNamed:] if creating overlays
    //UIImage *overlayImage = nil;
    for (int i = 0; i < [_rowAssets count]; ++i) {

        ELCAsset *asset = [_rowAssets objectAtIndex:i];

        if (i < [_imageViewArray count]) {
            UIImageView *imageView = [_imageViewArray objectAtIndex:i];
            imageView.image = [UIImage imageWithCGImage:asset.asset.thumbnail];
        } else {
            UIImageView *imageView = [[UIImageView alloc] initWithImage:[UIImage imageWithCGImage:asset.asset.thumbnail]];
            [_imageViewArray addObject:imageView];
        }
        
        if (i < [_overlayViewArray count]) {
            ELCAssetCellSelectedView *overlayView = [_overlayViewArray objectAtIndex:i];
            [self setTextOnOverlay:asset overlayView:overlayView];
            overlayView.hidden = asset.selected ? NO : YES;
        } else {
            ELCAssetCellSelectedView *overlayView = [[ELCAssetCellSelectedView alloc] initWithFrame:CGRectZero];
            overlayView.backgroundColor = [UIColor colorWithWhite:1.0f alpha:0.3f];
            overlayView.label = [[UILabel alloc] initWithFrame:CGRectZero];
            overlayView.label.backgroundColor = [UIColor colorWithRed:0.0f green:0.48f blue:1.0f alpha:1.0f];
            overlayView.label.textColor = [UIColor whiteColor];
            overlayView.label.textAlignment = NSTextAlignmentCenter;
            
            overlayView.border = [[UIView alloc] initWithFrame:CGRectZero];
            overlayView.border.layer.borderColor = overlayView.label.backgroundColor.CGColor;
            overlayView.border.layer.borderWidth = 5;
            
            [overlayView addSubview:overlayView.border];
            [overlayView addSubview:overlayView.label];
            
            
            [self setTextOnOverlay:asset overlayView:overlayView];
            
            [_overlayViewArray addObject:overlayView];
            overlayView.hidden = asset.selected ? NO : YES;
        }
    }
}

- (void)cellTapped:(UITapGestureRecognizer *)tapRecognizer
{
    CGPoint point = [tapRecognizer locationInView:self];
    CGFloat totalWidth = self.rowAssets.count * 75 + (self.rowAssets.count - 1) * 4;
    CGFloat startX = (self.bounds.size.width - totalWidth) / 2;
    
	CGRect frame = CGRectMake(startX, 2, 75, 75);
	
	for (int i = 0; i < [_rowAssets count]; ++i) {
        if (CGRectContainsPoint(frame, point)) {
            ELCAsset *asset = [_rowAssets objectAtIndex:i];
            [self.delegate didTapCell:asset sender:self];
            asset.selected = !asset.selected;
            ELCAssetCellSelectedView *overlayView = [_overlayViewArray objectAtIndex:i];
            overlayView.hidden = !asset.selected;
            [self setTextOnOverlay:asset overlayView:overlayView];
            break;
        }
        frame.origin.x = frame.origin.x + frame.size.width + 4;
    }
}

- (void)layoutSubviews
{    
    CGFloat totalWidth = self.rowAssets.count * 75 + (self.rowAssets.count - 1) * 4;
    CGFloat startX = (self.bounds.size.width - totalWidth) / 2;
    
	CGRect frame = CGRectMake(startX, 2, 75, 75);
	
	for (int i = 0; i < [_rowAssets count]; ++i) {
		UIImageView *imageView = [_imageViewArray objectAtIndex:i];
		[imageView setFrame:frame];
		[self addSubview:imageView];
        
        ELCAssetCellSelectedView *overlayView = [_overlayViewArray objectAtIndex:i];
        [overlayView setFrame:frame];
        [self addSubview:overlayView];
        
        overlayView.border.frame = overlayView.bounds;
        overlayView.label.frame = CGRectMake(frame.size.width - LABEL_SIZE, frame.size.height - LABEL_SIZE, LABEL_SIZE, LABEL_SIZE);
        
		frame.origin.x = frame.origin.x + frame.size.width + 4;
	}
}


@end
